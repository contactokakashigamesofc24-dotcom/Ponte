# Ponte

Mensageiro Android independente em desenvolvimento, usando a rede aberta Matrix e o SDK oficial Matrix Rust Kotlin. Não é Signal, Element, nem produto afiliado a esses projetos.

## Assinatura permanente do Android

Os APKs do workflow são assinados com uma chave permanente guardada como segredos criptografados do GitHub Actions no repositório. O código seleciona essa chave para os builds debug e release; o fluxo de publicação verifica a assinatura antes de enviar o APK. **Não apagar, substituir ou girar esses segredos:** uma mudança de chave impede o Android de aceitar atualizações sobre os APKs anteriores assinados por ela. Se os segredos forem perdidos, a chave antiga não poderá ser recuperada do GitHub.

O APK 0.2.4 é o primeiro com a chave permanente. O Android pode impedir que ele seja instalado por cima de uma versão anterior assinada com outra chave. Desinstalar a versão anterior apaga o armazenamento local e a sessão Matrix; confirme antes de remover qualquer versão que contenha dados que queira manter.

## Funcionalidades em teste

- Tela de boas-vindas e escolha do servidor Matrix antes de entrar.
- Cadastro Matrix aberto em navegador.
- Sessão e dados locais protegidos no aparelho.
- Lista de salas, criação de sala privada “Anotações”, convite a outro ID Matrix e mensagens de texto.

O app continua protótipo. Build e assinatura não comprovam funcionamento no telefone; login, sincronização, cifragem ponta a ponta e envio/recebimento precisam de teste real. Não use para conversas sensíveis ou como mensageiro de emergência.
