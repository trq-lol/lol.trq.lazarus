---
title: Shader packs
permalink: /docs/shader-packs.html
---
{% capture doc %}{% include_relative docs/core-renderer.md %}{% endcapture %}
{% assign status = doc | split: "## Status" | last | split: "## For render libraries" | first | replace: ".md)", ".html)" %}
# Shader packs

Shader packs run on the [core renderer](core-renderer.html). This is its current status, from the core renderer guide.

{{ status }}
