# Project writing rules: ASD-STE100 style

Use Simplified Technical English (STE) principles for all English text that users or developers read.

## Required rules
- Use short, clear sentences. Usually use 20 words or fewer per sentence.
- Use one instruction in each step. Put steps in the order that users must do them.
- Use the active voice. Name the person or component that does the action.
- Use a consistent word for one concept. Do not change between "slot", "appointment time", and "window" without a reason.
- Use approved words where possible. Use technical terms only when they are necessary. Define each new technical term at first use.
- Use present tense for facts. Use an imperative verb for instructions.
- State the result of each action when it helps the user.
- Make warnings direct. Explain the condition, risk, and action.
- Do not use idioms, promotional language, unexplained abbreviations, or vague terms such as "simply" or "obviously".
- Put conditions before actions. Example: "If the time is not available, select a different time."
- Keep UI labels, help text, error messages, CLI prompts, logs, onboarding instructions, API descriptions, and documentation in this style.
- Do not change identifiers, API property names, programming syntax, standard protocol names, or third-party error text to improve prose.
- Use user-selected languages for translated UI text. Apply equivalent clear-language rules to non-English translations.

## Review examples
| Avoid | Use |
| --- | --- |
| Your slot has vanished. | This time is not available. Select another time. |
| Something went wrong. | The server did not respond. Try again. |
| Bootstrap the services. | Start the services. |
| Seamlessly connect your calendar. | Connect your Google Calendar account. |
| Invalid configuration supplied. | The configuration is not valid. Check the fields. |

## Compliance limit
These rules adopt the **style** of ASD-STE100. They do not certify compliance with the ASD-STE100 specification. Formal compliance requires the approved dictionary, applicable writing rules, controlled vocabulary, and a trained review process. Do not claim certification.

## Pull request checklist
- [ ] Each new instruction has one clear action.
- [ ] UI messages explain what happened and what to do next.
- [ ] Technical terms are consistent.
- [ ] Warnings describe the risk.
- [ ] User-facing text follows the approved style guide.
