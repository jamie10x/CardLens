type ParsedContact = {
  fullName: string;
  company: string;
  jobTitle: string;
  email: string;
  phone: string;
  website: string;
  address: string;
  suggestedTags: string[];
};

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  const { text } = await req.json();
  const prompt = `Parse this business card OCR text into JSON with keys fullName, company, jobTitle, email, phone, website, address, suggestedTags. OCR text:\n${text}`;

  const apiKey = Deno.env.get("OPENAI_API_KEY");
  if (!apiKey) {
    return Response.json(fallbackParse(text));
  }

  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: Deno.env.get("OPENAI_MODEL") ?? "gpt-4.1-mini",
      input: prompt,
      text: {
        format: {
          type: "json_schema",
          name: "parsed_contact",
          schema: {
            type: "object",
            additionalProperties: false,
            properties: {
              fullName: { type: "string" },
              company: { type: "string" },
              jobTitle: { type: "string" },
              email: { type: "string" },
              phone: { type: "string" },
              website: { type: "string" },
              address: { type: "string" },
              suggestedTags: { type: "array", items: { type: "string" } },
            },
            required: ["fullName", "company", "jobTitle", "email", "phone", "website", "address", "suggestedTags"],
          },
        },
      },
    }),
  });

  const json = await response.json();
  const output = json.output?.[0]?.content?.[0]?.text;
  return Response.json(output ? JSON.parse(output) : fallbackParse(text));
});

function fallbackParse(text: string): ParsedContact {
  const lines = text.split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
  const email = text.match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i)?.[0] ?? "";
  const phone = text.match(/\+?\d[\d\s().-]{7,}\d/)?.[0] ?? "";
  const website = text.match(/(https?:\/\/)?(www\.)?[a-z0-9-]+\.[a-z]{2,}(\/[\w./-]*)?/i)?.[0] ?? "";
  return {
    fullName: lines[0] ?? "",
    jobTitle: lines[1] ?? "",
    company: lines[2] ?? "",
    email,
    phone,
    website,
    address: lines.at(-1) ?? "",
    suggestedTags: ["Lead"],
  };
}
