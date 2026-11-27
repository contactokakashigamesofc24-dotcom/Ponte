# Ponte 0.2.6 — componente TLS Android

Esta versão inclui a dependência Android JVM `org.rustls:rustls-platform-verifier:0.2.0`, publicada no Maven do projeto rustls, usada junto do rustls-platform-verifier 0.7.1 integrado ao SDK Matrix. O aplicativo inicializa o contexto Matrix Android antes da primeira chamada de rede. A validação de certificados TLS permanece ativa.

O APK usa a chave permanente do Ponte. A compilação e a verificação criptográfica da assinatura não comprovam uma conexão real; login, sincronização e mensagens ainda precisam ser testados no aparelho.
