---
title: Syntax
description: Arguments, numbers, dates, plurals, choices and quoting in ICU MessageFormat.
---

A message is ICU MessageFormat, the syntax of ICU and of the JavaScript `Intl` ecosystem, with references to other messages added (see [References](../references/index.md)). Everything outside braces is text.

## Arguments

| Written | Shows |
| --- | --- |
| `{name}` | The value: text as given, a number in the locale's default style, a date and time in the medium style, `true` or `false`, an enum constant's name. |
| `{count, number}` | A number in the locale's style: `1,234.5` in `en`, `1.234,5` in `de`. |
| `{count, number, integer}` | A rounded whole number. |
| `{share, number, percent}` | A percentage: `0.25` shows as `25%`. |
| `{price, number, currency}` | An amount in the locale's currency. |
| `{size, number, ::compact-short}` | A number skeleton of ICU, after `::`. |
| `{size, number, #,##0.0}` | A number pattern. |
| `{when, date}` | A date, in the `medium` style; `short`, `medium`, `long` and `full` choose another, `::yMMMd` a skeleton. |
| `{when, time}` | A time, in the same styles. |
| `{when, time, relative}` | The distance from now: `3 hours ago`, `in 2 days`. |

An argument's name starts with a letter or `_` and continues with letters, digits and `_`. A name of digits only, such as `{0}`, is a positional argument, for callers that give values by position.

Spaces around the parts are allowed: `{ count , number }` is `{count, number}`.

A date may be given as an instant, a date, a date and time with or without a zone; a date without a zone is shown in the zone the implementation is configured with.

## Plurals

```text
{count, plural, =0 {No items} one {# item} other {# items}}
```

A plural chooses a branch by a number:

1. a branch for the exact value, written `=0`, `=1`, `=42`, is chosen first;
2. otherwise the branch of the number's plural category in the message's language, by the rules of CLDR: `zero`, `one`, `two`, `few`, `many`, `other`;
3. otherwise `other`.

Inside a branch, `#` shows the number, formatted as by `{count, number}`. `#` belongs to the nearest plural only; inside a `select` nested in a plural, `#` is ordinary text.

Every plural has an `other` branch. Each language needs branches for its own categories: English uses `one` and `other`, Russian `one`, `few`, `many` and `other`, Arabic all six. A plural that lacks one of its language's categories is reported as `missing-plural-form`, and falls back to `other` for those numbers.

An offset subtracts from the number before its category is chosen and before `#` shows it, while exact values compare the number as given:

```text
{count, plural, offset:1 =0 {Nobody} =1 {Only {name}} one {{name} and # other} other {{name} and # others}}
```

`selectordinal` chooses by the ordinal categories of the language, for `1st`, `2nd`, `3rd`:

```text
{place, selectordinal, one {#st} two {#nd} few {#rd} other {#th}}
```

A plural whose value is not a number takes `other` and is reported as `invalid-value`.

## Choices

```text
{gender, select, female {her} male {his} other {their}} sword
```

A select chooses the branch named like the value, or `other`. A boolean value is `true` or `false`; an enum constant is its name, and its name in lowercase when no branch has the exact name. Every select has an `other` branch. A selector is any text without spaces or braces.

Plurals and selects nest without limit inside each other's branches.

## Quoting

`'` before `{`, `}`, `|`, or `#` inside a plural branch, starts quoted text, which ends at the next single `'`; inside it, braces and `#` are ordinary characters. `''` is one apostrophe anywhere. Any other apostrophe is an ordinary character, so `It's` needs no quoting.

| Written | Shows |
| --- | --- |
| `It's {name}'s turn` | `It's Ann's turn` |
| `Use '{name}' to insert a name` | `Use {name} to insert a name` |
| `It''s fine` | `It's fine` |
| `{count, plural, one {'#'1} other {'#'#}}` | `#1`, `#3` |

## Not valid

These are `syntax` issues; the message is left out (see [Issues](../issues/index.md)):

- an unclosed `{` or a `}` without an opening one;
- an argument without a name, such as `{}`;
- an unknown type, such as `{when, duration}`;
- a plural or select without `other`, or with a branch twice;
- a plural branch that is neither an exact value nor a CLDR category, such as `single`;
- an empty style, such as `{count, number, }`.

ICU's `choice` type and the `spellout`, `ordinal` and `duration` types are not part of the format.
