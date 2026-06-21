---
name: jira-ticket-reader
description: "Retrieve complete Jira issue details using the jira-mcp MCP server. Use when users provide Jira keys (for example PROJ-123), ask to read/fetch/retrieve ticket details, request Jira fields for QA, automation, BDD, or analysis, or provide JQL for Jira searches. Return Jira content exactly as retrieved without summarization or interpretation."
---

# Jira Ticket Reader

## Purpose

This skill retrieves Jira ticket data from Jira via the `jira-mcp` MCP server and returns source content exactly as provided by Jira.

Jira is the source of truth. Do not add, infer, summarize, or rewrite requirements.

## When To Use

Use this skill when any of the following is true:

- A Jira issue key is provided directly (for example `PROJ-123`).
- A Jira issue key appears in free-form text.
- The user asks to fetch, read, retrieve, or include Jira ticket details.
- Another agent needs raw Jira ticket content as input.

### Example Triggers

- `PROJ-123`
- `Read Jira ticket PROJ-123`
- `Generate test cases for PROJ-123`
- `Create automation for PROJ-123`
- `Retrieve PROJ-123 and include Priority and Sprint`
- `Run JQL: project = PROJ AND status = \"In Progress\" ORDER BY updated DESC`

## Do Not Use

Do not use this skill when the user asks only for Jira-independent brainstorming or generic templates and does not request Jira retrieval.

## Retrieval Rules

1. Parse all Jira issue keys from the request using a robust pattern like `[A-Z][A-Z0-9]+-[0-9]+`.
2. Deduplicate extracted keys while preserving encounter order.
3. Use `get_issue` for direct ticket retrieval.
4. Use `jql_search` only if:
   - The user explicitly asks for a search, or
   - The user provides a JQL query, or
   - Direct key retrieval is not possible and search is required.
5. Always attempt to retrieve these fields when available:
   - Summary
   - Description
   - Acceptance Criteria
   - Comments
6. For EPAM Jira instances (`jiraeu.epam.com`), Acceptance Criteria is stored in `customfield_12700`.
   Always explicitly include `customfield_12700` in the `fields` array when calling `get_issue` against EPAM Jira.
   Map `customfield_12700` to the `ACCEPTANCE CRITERIA` output section.
7. Retrieve additional fields explicitly requested by the user.
7. If a requested field does not exist or is empty, omit it from output.
8. Preserve all values exactly as returned by Jira.

## Data Integrity Requirements

Never do the following:

- Summarize, simplify, or paraphrase Jira content.
- Rewrite acceptance criteria or description text.
- Infer missing details.
- Generate placeholders.
- Merge multiple fields into interpreted prose.
- Truncate long field values.
- Remove formatting such as lists, tables, code blocks, links, or line breaks.

Always preserve:

- Line breaks
- Ordered and unordered lists
- Tables
- Markdown/wiki formatting
- Code blocks
- URLs

## Output Contract

Return only retrieved Jira data using this structure:

```text
SKILL_EXECUTED: jira-ticket-reader

JIRA_METADATA

Ticket Key:
<Issue Key>

Issue Type:
<Issue Type>

Project:
<Project>

SUMMARY

<Raw Summary>

DESCRIPTION

<Raw Description>

ACCEPTANCE CRITERIA

<Raw Acceptance Criteria>

COMMENTS

<Comment 1>

<Comment 2>

<Comment N>

ADDITIONAL_FIELDS

<Field Name>
<Field Value>
```

## Output Rules

- The first output line must always be exactly: `SKILL_EXECUTED: jira-ticket-reader`.
- Omit sections that are unavailable.
- Never output placeholder text.
- For multiple ticket keys, repeat the full output block per ticket in the same order as requested.
- If no valid Jira key or JQL is found, ask for a Jira key or explicit JQL query.

## Error Handling

- If Jira retrieval fails, return the exact tool error context relevant to retrieval and ask for the next corrective input (for example, confirm key, access, or project scope).
- If partial data is available, return available sections and omit missing ones.
- If permissions restrict fields/comments, return only what is accessible without speculation.

## Best Practices

- Prefer explicit field requests when users ask for specific metadata (for example Priority, Sprint, Labels, Assignee, Status).
- Keep retrieval deterministic: source data first, analysis later.
- For downstream tasks (tests, automation, BDD), perform retrieval first, then pass raw output to the next step.
- Maintain strict separation between retrieval output and any later interpretation.

## Good vs Bad Behavior

### Good

User input:

```text
Generate test cases for PROJ-123
```

Expected action:

- Extract `PROJ-123`
- Call `get_issue`
- Return raw Jira fields in the defined output contract

### Good

User input:

```text
Retrieve PROJ-123 and include Priority and Sprint
```

Expected action:

- Retrieve default fields
- Retrieve Priority and Sprint
- Return raw values exactly as retrieved

### Bad

Do not return a summary like:

```text
This story allows users to manage orders.
```

when Jira contains richer original content.

Always return original Jira data.
