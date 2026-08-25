// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.markdown

import android.app.Activity
import android.graphics.Color
import android.view.ViewGroup.LayoutParams
import com.hive.plugin.ComponentManager
import com.hive.plugin.provider.IEditorProvider

object MarkdownHelper {

    val isTest = true;

    val test = """
![随机风景图](https://img.colorhub.me/zb-TgrWoYHg/rs:auto:0:500:0/g:ce/fn:colorhub/bG9jYWw6Ly8vMDUvZjcvZjdmNzM1ODE0ZDc4ODExZWQxMjllZDY3ODkyZWU2ZWQyNDc5MDVmNy5qcGc.webp)
   ```markdown

   ```markdown
# 🚀 Markdown 测试文档

## 📊 Mermaid 流程图示例
```mermaid
graph TD
    A[开始] --> B{条件判断}
    B -->|是| C[执行操作1]
    B -->|否| D[执行操作2]
    C --> E[结束]
    D --> E
```

针对创业初期控制权保护与融资需求，为您设计「金字塔型控制架构方案」，分三个阶段实施：

### 一、初创期架构（注册资本500万以内）
**核心策略**：预留股权池+有限合伙控制  
```mermaid
graph TD
    A[创始人] --> B[有限责任公司-母公司]
    B --> |GP出资1%|C[有限合伙企业-员工持股平台]
    B --> |GP出资1%|D[有限合伙企业-投资人平台]
    C --> |LP99%|E[核心团队]
    D --> |LP99%|F[天使投资人]
```

**关键操作**：  
1. **母公司实控**：创始人直接持有母公司67%股权，剩余33%用于未来融资  
2. **持股平台设计**：  
   - 创始人担任两个有限合伙企业的普通合伙人（GP），仅需1%出资即获得100%控制权  
   - 员工通过持股平台获得分红权，无表决权  
3. **协议加固**：  
   - 签订《一致行动人协议》约束创始团队  
   - 投资人签署《投票权委托协议》  

### 二、成长期架构（A轮融资后）
**核心策略**：AB股+董事会控制权  
```mermaid
graph TD
    G[境外开曼公司] --> H[香港公司]
    H --> I[境内WFOE]
    I --> J[协议控制] --> K[运营实体]
    G --> |1:10投票权|L[创始人B股]
    G --> |1:1投票权|M[投资人A股]
```

**关键操作**：  
1. **搭建VIE架构**（如需境外上市）：  
   - 创始人通过AB股持有开曼公司67%投票权  
   - 投资人的股权对应经济权益，但投票权比例受限  
2. **境内控制**：  
   - 保留母公司作为境内主体，通过协议控制运营实体  
   - 公司章程设置「重大事项一票否决权」条款  

### 三、成熟期架构（Pre-IPO阶段）
**核心策略**：多层防火墙+动态股权调整  
```mermaid
graph TD
    N[家族信托] --> O[控股集团公司]
    O --> P[产业子公司A]
    O --> Q[产业子公司B]
    O --> R[有限合伙基金]
    R --> S[产业链投资]
```
**关键操作**：  
1. **资产隔离**：  
   - 通过家族信托持有控股公司股权  
   - 各业务板块独立子公司运营  
2. **控制权工具**：  
   - 发行可转换优先股融资  
   - 设计「金股」制度（对重大决策有超级投票权）  

### 📍 核心控制权保护要点
1. **股权比例红线**：  
   - 67%（绝对控制）  
   - 51%（相对控制）  
   - 34%（重大事项否决权）  

2. **公司章程特别条款**：  
   ```
   - 第X条：创始人提名的董事占董事会席位2/3  
   - 第Y条：公司合并/分立需90%以上股东同意  
   - 第Z条：创始人股份转让限制（优先受让权）  
   ```

3. **融资条款禁区**：  
   ⚠️ 禁止签署：  
   - 对赌协议（如必须签，限定个人责任）  
   - 优先清算权超过2倍  
   - 领售权（Drag-Along）触发条件低于50%  
![随机风景图](https://img.colorhub.me/zb-TgrWoYHg/rs:auto:0:500:0/g:ce/fn:colorhub/bG9jYWw6Ly8vMDUvZjcvZjdmNzM1ODE0ZDc4ODExZWQxMjllZDY3ODkyZWU2ZWQyNDc5MDVmNy5qcGc.webp)
### 💼 配套法律文件清单
1. 《股东协议》——约定股权成熟机制（4年解锁期）  
2. 《投票权委托协议》——代持股份需公证  
3. 《配偶承诺函》——避免婚变导致股权分割  

需要帮您测算不同融资轮次的股权稀释模型吗？ 😊 建议同步咨询公司法律师设计具体条款。
""".trimIndent()

    fun test(context: Activity) {
        val textView = MarkdownTextView(context)
        textView.setBackgroundColor(Color.WHITE)
        //添加到根布局
        context.addContentView(
            textView,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        )
        textView.post {


            textView.loadMarkdown(test)
        }
    }

    fun test2() {
        val provider2 = ComponentManager.getInstance()
            .getProvider(IEditorProvider::class.java) as IEditorProvider?
        Thread {
            val bmp = provider2?.renderMarkdownToBitmap(test)

        }.start()
    }
}