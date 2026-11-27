# Ponte 0.2.5 — correção inicialização Android Matrix

O app agora inicializa `org.matrix.rustsdk.Android.init(applicationContext)` antes de `initPlatform` e de qualquer chamada de rede. Esse é o caminho indicado pelo próprio Matrix Rust SDK para iniciar a verificação TLS Android; não se desativa a validação de certificados.

O APK é assinado com a chave permanente configurada no GitHub Actions. O build e a checagem de assinatura não provam login real; instale por cima da 0.2.4 e teste no aparelho. Não digite senha se surgir um erro de inicialização. Não use para conversas sensíveis antes de validação.
