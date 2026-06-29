import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { mobile } = await req.json();

    if (!mobile) {
      return new Response(JSON.stringify({ error: "Mobile number is required." }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const authKey = Deno.env.get("MSG91_AUTH_KEY");
    const templateId = Deno.env.get("MSG91_TEMPLATE_ID");

    if (!authKey || !templateId) {
      return new Response(JSON.stringify({ error: "MSG91 credentials are not configured on the server." }), {
        status: 500,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // Call MSG91 API to send OTP
    const url = `https://control.msg91.com/api/v5/otp?template_id=${templateId}&mobile=${mobile}`;
    
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "authkey": authKey,
        "Content-Type": "application/json"
      }
    });

    const msg91Data = await response.json();

    if (msg91Data.type === "success") {
      return new Response(JSON.stringify({ message: "OTP sent successfully", data: msg91Data }), {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    } else {
      return new Response(JSON.stringify({ error: "Failed to send OTP via MSG91", details: msg91Data }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

  } catch (error) {
    return new Response(JSON.stringify({ error: "Internal Server Error", details: error.message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
