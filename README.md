# Ponte

Mensageiro Android independente em desenvolvimento, usando a rede aberta Matrix e o SDK oficial Matrix Rust Kotlin. Não é Signal, Element, nem produto afiliado a esses projetos.

## Versão 0.2.3 — protótipo em teste

- Tela própria de boas-vindas e fluxo em etapas: escolher servidor Matrix, depois informar usuário e senha.
- Cadastro Matrix aberto em aba do navegador para evitar que o Android encaminhe o link ao app Element.
- Login Matrix (matrix.org por padrão), sessão protegida pelo Android Keystore e armazenamento local protegido por chave aleatória.
- Sincronização, lista de salas, sala privada “Anotações”, convite a uma pessoa pelo ID Matrix e mensagens de texto.

**Ainda é um protótipo.** Build concluído; login real, sincronização, criptografia ponta a ponta e mensagens ainda precisam de teste no aparelho. Não use para conversas sensíveis nem como mensageiro de emergência.
