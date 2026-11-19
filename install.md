---
title: Install
---
{% capture readme %}{% include_relative README.md %}{% endcapture %}
{% assign section = readme | split: "## Install" | last | split: "## LWJGL 3" | first | replace: ".md)", ".html)" %}
# Install

{{ section }}
