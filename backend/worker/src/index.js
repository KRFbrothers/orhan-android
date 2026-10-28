// Orhan /chat — Cloudflare Worker proxy to OpenRouter. Orhan-only secrets.
export default {
  async fetch(req, env) {
    const url = new URL(req.url);
    if (url.pathname !== "/chat" || req.method !== "POST")
      return new Response("Not found", { status: 404 });
    if (req.headers.get("X-Orhan-Key") !== env.ORHAN_APP_KEY)
      return new Response("Unauthorized", { status: 401 });
    let body;
    try { body = await req.json(); } catch { return json({ error: "bad json" }, 400); }
    const msgs = Array.isArray(body.messages) ? body.messages.slice(-20) : null;
    if (!msgs || JSON.stringify(msgs).length > 8000) return json({ error: "invalid messages" }, 400);
    const r = await fetch("https://openrouter.ai/api/v1/chat/completions", {
      method: "POST",
      headers: { Authorization: `Bearer ${env.OPENROUTER_API_KEY}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        model: env.MODEL,
        messages: [{ role: "system", content: "You are Orhan, a helpful assistant." }, ...msgs],
      }),
    });
    if (!r.ok) return json({ error: "upstream", status: r.status }, 502);
    const d = await r.json();
    return json({ reply: d.choices?.[0]?.message?.content ?? "" });
  },
};
const json = (o, s = 200) => new Response(JSON.stringify(o), { status: s, headers: { "Content-Type": "application/json" } });
