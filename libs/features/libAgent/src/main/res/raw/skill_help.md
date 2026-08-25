## Skill MCP Tool

Flow: `help` (recommended, includes current skill list) → `run` with id. Or `list` when you specifically need the structured list.

### Actions


| action | Required                                                  | Optional                                                                      |
| ------ | --------------------------------------------------------- | ----------------------------------------------------------------------------- |
| help   | action                                                    | -                                                                             |
| list   | action                                                    | -                                                                             |
| run    | action, userPrompt                                        | id, options. 无 id 时 inline: name, description, systemPrompt, allowedToolNames |
| create | action, name, description, systemPrompt, allowedToolNames | id, maxRounds, timeoutMs, memoryGroup, fallbackSkillId                        |
| update | action, id                                                | 任意 patch 字段                                                                   |
| delete | action, id                                                | -                                                                             |


**默认值**: maxRounds/timeoutMs 默认 -1（不限制），memoryGroup 可省略。create 时 id 省略则自动生成 skill.xxxxxxxx（8 位随机）。

### Examples

```json
{"action":"help"}
{"action":"list"}
```

**run (by id)**

```json
{"action":"run","id":"demo","userPrompt":"Use this skill to complete a task"}
```

**run (inline)** 临时执行，不持久化

```json
{"action":"run","userPrompt":"...","name":"Ad-hoc","description":"...","systemPrompt":"...","maxRounds":-1,"timeoutMs":-1,"allowedToolNames":["buildin.interact","buildin.input"]}
```

**create**（id 可选，省略时自动生成 8 位随机 ID）

```json
{"action":"create","name":"Demo","description":"...","systemPrompt":"...","maxRounds":-1,"timeoutMs":-1,"allowedToolNames":["buildin.interact","buildin.input"]}
```

**update** 仅传需修改的字段

```json
{"action":"update","id":"demo","description":"New description"}
```

**delete**

```json
{"action":"delete","id":"demo"}
```
