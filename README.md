# Ponte 0.2.7 — diagnóstico do login Matrix

A tela de erro de login agora mostra a causa/código Matrix devolvido pelo SDK, com tentativa de ocultar tokens/segredos; não mostra senha. O ID digitado permanece preenchido após falha e o campo de senha é limpo. A dependência rustls-platform-verifier do Android continua incluída, com validação TLS ativa.

Build assinado com a chave permanente. Compilar e verificar assinatura não prova conexão Matrix real.
