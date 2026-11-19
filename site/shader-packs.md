---
title: Shader packs
permalink: /docs/shader-packs.html
---
{% capture doc %}{% include_relative docs/core-renderer.md %}{% endcapture %}
{% assign status = doc | split: "## Shader packs" | last | split: "## How it works" | first | replace: ".md)", ".html)" %}
# Shader packs

Shader packs run on the [core renderer](core-renderer.html). From the core renderer guide:

{{ status }}
