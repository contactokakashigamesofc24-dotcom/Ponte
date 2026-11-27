# Ponte

Mensageiro Android independente em desenvolvimento, usando a rede aberta Matrix e o SDK oficial Matrix Rust Kotlin. Não é Signal, Element, nem produto afiliado a esses projetos.

## Estado atual

Versão 0.2.0 em desenvolvimento:
- login Matrix com servidor configurável (matrix.org por padrão);
- armazenamento de sessão criptografado com chave mantida pelo Android Keystore;
- banco local do SDK usando chave aleatória;
- sincronização inicial e listagem de salas ingressadas;
- visualização de mensagens de texto e envio em salas existentes.

O projeto ainda é protótipo e precisa de testes reais de login, sincronização, cifragem ponta a ponta e envio/recebimento em duas contas antes de ser considerado funcional e pronto para uso cotidiano. Não use como cliente de emergência nem para informações sensíveis até a revisão e validação.

Para criar uma conta, o botão abre o cadastro do Matrix.org no navegador. A senha é digitada no próprio aparelho e não deve ser enviada por chat. Token e chave local são protegidos com Android Keystore; o código-fonte é público.

## Compilação

GitHub Actions compila APK de depuração e publica-o como pré-lançamento. Veja Actions/Releases no repositório.
