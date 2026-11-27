# Ponte 0.2.8 — entrada Matrix via SSO

Adiciona uma opção inicial “Entrar pelo navegador (SSO)”. O SDK inicia o SSO Matrix no homeserver escolhido, abre a página de autenticação no navegador do sistema/Custom Tab e recebe o retorno no deep link `ponte://login`. Após concluir, o SDK persiste a sessão pelo cofre Android já utilizado no login por senha. O app não lê nem transmite a senha do provedor SSO.

O fluxo depende do homeserver e do provedor configurarem o callback de maneira compatível. A compilação/assinatura não confirmam ainda a autenticação real; teste no aparelho antes de usar a conta. A opção de login por senha permanece disponível.
