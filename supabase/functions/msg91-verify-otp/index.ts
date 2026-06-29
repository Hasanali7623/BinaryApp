import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.7.1';

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { mobile, otp } = await req.json();

    if (!mobile || !otp) {
      return new Response(JSON.stringify({ error: "Mobile number and OTP are required." }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const authKey = Deno.env.get("MSG91_AUTH_KEY");
    if (!authKey) {
      return new Response(JSON.stringify({ error: "MSG91 credentials are not configured." }), {
        status: 500,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // 1. Verify OTP with MSG91
    const url = `https://control.msg91.com/api/v5/otp/verify?otp=${otp}&mobile=${mobile}`;
    const response = await fetch(url, {
      method: "GET",
      headers: { "authkey": authKey }
    });

    const msg91Data = await response.json();

    if (msg91Data.type !== "success") {
      return new Response(JSON.stringify({ error: "Invalid or expired OTP", details: msg91Data }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // 2. OTP is verified! Now handle the user in Supabase.
    // Connect to Supabase via Service Role to bypass RLS
    const supabaseUrl = Deno.env.get('SUPABASE_URL') || '';
    const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') || '';
    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    // Check if user exists in our custom users table
    let { data: user, error: fetchError } = await supabase
      .from('users')
      .select('*')
      .eq('phone', mobile)
      .single();

    if (!user) {
      // Create new user
      const { data: newUser, error: insertError } = await supabase
        .from('users')
        .insert([{ 
          phone: mobile, 
          role: 'user', 
          created_at: new Date().toISOString() 
        }])
        .select()
        .single();

      if (insertError) {
        throw new Error("Failed to create new user: " + insertError.message);
      }
      user = newUser;
    }

    // In a production app, we would generate a JWT token here.
    // For this REST setup, we return the user profile.
    return new Response(JSON.stringify({ 
      message: "Authentication successful", 
      user: user,
      session_token: "mock_token_for_rest_implementation_" + user.id
    }), {
      status: 200,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });

  } catch (error) {
    return new Response(JSON.stringify({ error: "Internal Server Error", details: error.message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
