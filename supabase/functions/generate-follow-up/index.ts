Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  const contact = await req.json();
  const firstName = String(contact.fullName ?? "there").split(" ")[0];
  const fallback = {
    message: `Hi ${firstName}, great meeting you. I enjoyed learning about ${contact.company || "your work"}. Are you available this week for a quick call?`,
  };

  const apiKey = Deno.env.get("OPENAI_API_KEY");
  if (!apiKey) {
    return Response.json(fallback);
  }

  const prompt = `Write a concise professional follow-up message for this networking contact. Use a warm tone and reference the context if available.\n${JSON.stringify(contact)}`;
  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: Deno.env.get("OPENAI_MODEL") ?? "gpt-4.1-mini",
      input: prompt,
    }),
  });

  const json = await response.json();
  const message = json.output?.[0]?.content?.[0]?.text;
  return Response.json({ message: message ?? fallback.message });
});
