# Copilot Instructions

## Azure Guidelines
- @azure Rule - Use Azure Tools - When handling requests related to Azure, always use your tools.
- @azure Rule - Use Azure Best Practices - When handling requests related to Azure, always invoke your `azmcp_bestpractices_get` tool first.
- @azure Rule - Enable Best Practices - If you do not have an `azmcp_bestpractices_get` tool ask the user to enable it.

## Application Development Guidelines
- When the user asks for changes, start by updating the specification first; only update the code after the specification is confirmed or accepted.
- For new app instances, ask for a Name and use it as the app name in the format '{Name} - Virtual Detector'.
