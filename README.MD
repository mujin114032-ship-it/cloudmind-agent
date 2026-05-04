第一阶段：基础 RAG 闭环
已完成：文档上传、解析、分块、向量化、Milvus 入库、检索测试、普通问答、流式问答

具体流程：
知识库管理
↓
文档上传
↓
文档解析
↓
文本分块
↓
Embedding 向量化
↓
Milvus 向量入库
↓
向量检索
↓
Prompt 组装
↓
大模型问答
↓
SSE 流式输出
↓
前端展示回答与参考片段

具体实现：
1. 知识库 CRUD
2. 知识库文档绑定
3. 真实文档上传
4. Tika / PDFBox 文档解析
5. 扫描版 PDF 失败识别
6. 文本滑动窗口分块
7. document_chunk 落库
8. bge-base-zh-v1.5 本地 Embedding 服务
9. Spring Boot 调用 FastAPI Embedding
10. Milvus Collection 初始化
11. 向量写入 Milvus
12. 文档入库状态更新
13. 知识库向量检索测试
14. 普通 RAG 问答
15. SSE 流式 RAG 问答
16. 前后端联桥测试