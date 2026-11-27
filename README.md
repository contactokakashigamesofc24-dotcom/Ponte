# Ponte

Mensageiro Android independente em desenvolvimento, usando a rede aberta Matrix e o SDK oficial Matrix Rust Kotlin. Não é Signal, Element, nem produto afiliado a esses projetos.

## Versão 0.2.0 — integração inicial em teste

- Login com uma conta Matrix existente, com servidor configurável (matrix.org por padrão).
- Guarda a sessão cifrada usando uma chave do Android Keystore e mantém o banco local protegido por chave aleatória.
- Inicia sincronização e mostra salas em que a conta entrou.
- Permite criar a sala privada criptografada “Anotações” para testar no mesmo aparelho.
- Permite iniciar uma conversa privada com convite a outro identificador Matrix.
- Abre salas e envia/recebe mensagens de texto.

**Ainda é um protótipo.** A compilação passou, mas login real, sincronização, criptografia ponta a ponta e mensagens precisam ser testados no aparelho antes de considerar o app pronto. Não use para conversas sensíveis ou como mensageiro de emergência.

### Teste com um aparelho

1. Entre com uma conta Matrix. Se não tiver, use “Criar uma conta Matrix”, que abre o cadastro do provedor no navegador.
2. Toque em “Criar Anotações” para criar uma sala privada e enviar uma mensagem para você mesmo.
3. Para conversar com outra pessoa, toque em “Nova conversa” e digite o ID Matrix dela, por exemplo `@nome:matrix.org`. Ela precisa ter uma conta Matrix; não precisa instalar Ponte.

Use sua senha Matrix somente no app Ponte, não a senha do Gmail. Tokens de sessão ficam protegidos pelo Android Keystore. O código-fonte deste repositório é público.

## Build

O GitHub Actions compila um APK de depuração e publica pré-lançamentos. Todos os builds ainda exigem validação no aparelho.
