---
title: Install
---
{% capture readme %}{% include_relative README.md %}{% endcapture %}
{% assign section = readme | split: "## Install" | last | split: "## LWJGL 3" | first | replace: ".md)", ".html)" %}
# Install

{{ section }}

## Use with Claude Code

The [trq-lol plugin](https://github.com/trq-lol/claude-plugin) teaches Claude Code to install, configure and troubleshoot Lazarus.

```
/plugin marketplace add trq-lol/claude-plugin
/plugin install trq-lol@trq-lol
```
