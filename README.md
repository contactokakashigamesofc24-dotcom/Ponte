# Ponte

Mensageiro Android independente em desenvolvimento, usando a rede aberta Matrix e o SDK oficial Matrix Rust Kotlin. Não é Signal, Element, nem produto afiliado a esses projetos.

## Versão 0.2.1 — integração inicial em teste

- Tela de boas-vindas própria, com fluxo inspirado visualmente na referência enviada, sem marca ou logotipo do Element.
- Login com conta Matrix existente, com servidor configurável (matrix.org por padrão).
- Guarda a sessão cifrada usando chave do Android Keystore e banco local do SDK protegido por chave aleatória.
- Inicia sincronização e mostra salas em que a conta entrou.
- Permite criar a sala privada criptografada “Anotações” para testar no mesmo aparelho.
- Permite iniciar conversa privada e convidar outra pessoa pelo ID Matrix.
- Abre salas e envia/recebe mensagens de texto.

**Ainda é um protótipo.** A compilação passa, mas login real, sincronização, criptografia ponta a ponta e mensagens precisam ser testados no aparelho antes de considerar o app pronto. Não use para conversas sensíveis ou como mensageiro de emergência.

### Teste com um aparelho

1. Na abertura, toque em “Entrar manualmente” e entre com uma conta Matrix. Se não tiver, “Criar conta Matrix” abre o cadastro no navegador.
2. Toque em “Criar Anotações” para criar uma sala privada e enviar mensagem para você mesmo.
3. Para conversar com outra pessoa, toque em “Nova conversa” e digite o ID Matrix dela, por exemplo `@nome:matrix.org`. Ela precisa ter uma conta Matrix; não precisa instalar Ponte.

Use sua senha Matrix somente no app Ponte, não a senha do Gmail. Tokens de sessão ficam protegidos pelo Android Keystore. O código-fonte do repositório é público.

## Build

O GitHub Actions compila um APK de depuração e publica pré-lançamentos. Todos os builds ainda exigem validação no aparelho.
