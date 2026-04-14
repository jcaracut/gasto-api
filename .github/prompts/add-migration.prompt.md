---
description: "Create a new Flyway SQL migration for the Expense Tracker API"
argument-hint: "Describe the schema change, e.g. 'Add currency VARCHAR column to expenses table'"
agent: "agent"
---
Create a new Flyway migration for the Gasto Expense Tracker API.

## Change Required
$input

## Steps

1. **Read existing migrations** in `src/main/resources/db/migration/` to find the highest `V{n}` version
2. **Determine the next version**: `V{n+1}`
3. **Read the affected domain entity** in `src/main/java/com/expensetracker/domain/` to understand current mapping

## SQL Rules to Follow
- File name: `V{n+1}__short_snake_case_description.sql`
- All timestamps: `TIMESTAMPTZ`
- All IDs: `UUID`
- All monetary values: `NUMERIC(15, 2)`
- Add `ON DELETE CASCADE` or `ON DELETE RESTRICT` on every FK ? choose based on semantic
- Add an index on every new column used in `WHERE`, `JOIN`, or `ORDER BY`
- Use `ADD COLUMN IF NOT EXISTS` / `DROP COLUMN IF EXISTS` guards
- NEVER drop a column or table unless explicitly instructed
- NEVER edit an existing migration file

## Output
1. The full path of the new file
2. The complete SQL content
3. A one-line summary of which domain entity fields need updating (if any) ? do NOT write Java code here
