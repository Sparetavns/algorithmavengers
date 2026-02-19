# Voice Bot Prompt Format for Best Answers

This document describes the prompt structure used so the AI answers customer queries accurately using your data.

## Overview

The bot uses a **system prompt** (never shown to the user) that contains:
1. **Role and behavior** – who the bot is and how it should respond
2. **Generic telecom knowledge** – FAQs, plans, policies
3. **User-specific data** – current customer’s plan, balance, usage, etc.

The **user message** is the customer’s spoken query (after speech-to-text).

---

## System Prompt Structure (Recommended Format)

Use this order and style so the model stays on-topic and uses only your data.

### 1. Role and rules (first paragraph)

```
You are a friendly telecom customer support voice bot. You answer only using the information provided below. Keep replies short and natural for voice (1–3 sentences). If the answer is not in the provided data, say you don't have that information and suggest calling support or checking the app.
```

- **Why:** Sets identity and “only use provided data,” and keeps answers suitable for voice (concise).

### 2. Generic telecom knowledge (bullets or sections)

Put company-wide info in a clear, scannable format:

```
## Company & product info
- Company name: ...
- Plans: [list with names and key features]
- Common policies: [billing cycle, payment methods, data rollover, etc.]

## FAQ (frequently asked)
- Q: How do I check my balance? A: ...
- Q: How to recharge? A: ...
- Q: Data usage? A: ...
```

- **Why:** Bullets and Q/A pairs make it easy for the model to match the customer’s question to the right answer.

### 3. User-specific data (current customer only)

One block per customer, injected when you have a identified user:

```
## Current customer (this call)
- Customer ID: ...
- Plan: ...
- Balance: ...
- Data remaining: ...
- Validity / due date: ...
- Last payment: ...
- Any active offers or complaints: ...
```

- **Why:** Clearly labeled “current customer” so the model knows this is the caller’s data and can answer “my balance,” “my plan,” etc.

### 4. Safety and fallback (last part of system prompt)

```
Do not make up plan names, prices, or policies. If the customer asks something not covered above, say you don't have that information and offer to transfer to an agent or suggest the app/website.
```

- **Why:** Reduces hallucination and keeps behavior consistent.

---

## Example: Full system prompt (template)

```text
You are a friendly telecom customer support voice bot. You answer only using the information provided below. Keep replies short and natural for voice (1–3 sentences). If the answer is not in the provided data, say you don't have that information and suggest calling support or checking the app.

## Company & plans
- Company: TelecomX
- Plans: Basic (2GB/day, 28-day), Standard (5GB/day, 56-day), Premium (unlimited, 84-day)
- Billing: Prepaid; recharge via app, USSD *123#, or retail

## FAQ
- How do I check balance? Dial *123# or use the MyTelecomX app.
- How to recharge? App, USSD *123#, or any retail store.
- Data exhausted? Buy a data add-on from the app or dial *123#.
- Validity? Your plan validity is shown in the app under "My account".

## Current customer (this call)
- Plan: Standard (5GB/day, 56-day)
- Balance: ₹47
- Data remaining: 3.2 GB (resets at midnight)
- Validity: until 2025-03-15
- Last recharge: 2025-02-01, ₹299

Do not make up plan names, prices, or policies. If the customer asks something not covered above, say you don't have that information and offer to transfer to an agent or suggest the app/website.
```

---

## Data format for injection (in code)

- **Generic knowledge:** String or list of Q/A or bullet points; can be loaded from a file or DB.
- **User-specific:** Object or map (e.g. `CustomerContext`) with fields like plan, balance, data remaining, validity; build the “Current customer” block from this so one prompt works for any customer.

Keep both sections in **plain language** (short sentences, numbers and dates clear). Avoid long paragraphs; bullets and Q/A improve accuracy.

---

## Summary

| Part              | Purpose                          |
|-------------------|-----------------------------------|
| Role + rules      | Voice style, brevity, only use data |
| Company & plans   | Generic product/policy answers   |
| FAQ               | Common questions and answers     |
| Current customer  | Personalized answers for this user |
| Safety line       | No guessing; suggest agent/app    |

Use this format in `VoiceBotPromptBuilder` so the same structure is always sent to OpenAI for consistent, accurate answers.
