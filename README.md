# Ponte

Mensageiro Android independente em desenvolvimento, usando a rede aberta Matrix e o SDK oficial Matrix Rust Kotlin. Não é Signal, Element, nem produto afiliado a esses projetos.

## Versão 0.2.2 — protótipo em teste

- Tela própria de boas-vindas e fluxo em etapas: escolher servidor Matrix, depois informar usuário e senha.
- Login Matrix com servidor configurável (matrix.org por padrão).
- Sessão cifrada com chave do Android Keystore e armazenamento local protegido por chave aleatória.
- Sincronização, lista de salas, criação de sala privada “Anotações”, convite a uma pessoa pelo ID Matrix e mensagens de texto.

**Ainda é um protótipo.** O build compila; login real, sincronização, cifragem ponta a ponta e mensagens ainda precisam de teste no aparelho. Não use para conversas sensíveis nem como mensageiro de emergência.

Use uma senha Matrix apenas no app Ponte, nunca a senha do Gmail. Criar conta abre o cadastro do provedor Matrix no navegador.
