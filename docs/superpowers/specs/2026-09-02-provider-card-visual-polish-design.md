# 模型服务卡片视觉打磨

## Goal
美化「模型与服务 → 模型服务」Provider 卡片：品牌可识别 logo、底部操作 icon、轻微间距与层级优化。

## Decisions
- Logo：品牌识别版简化矢量（非统一线稿、非位图）
- 展示：40dp 浅色圆底 + 24dp 标
- 底部：矢量加号 + expand_more；热区 44dp；与文案间距加大
- 不改交互/业务逻辑

## Out of scope
大改页面结构、位图品牌包、未映射厂商的精细定制（走 default）
