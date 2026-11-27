# Ponte

Projeto Android independente de mensageiro, iniciado em 27/11/2026. Element X é apenas referência funcional/visual; este projeto não reutiliza a identidade visual nem o código do Element ou do Lume.

## Estado atual

A primeira entrega é um protótipo nativo em Kotlin + Jetpack Compose com:

- tela inicial Ponte;
- botão para abrir o cadastro oficial Matrix.org no navegador;
- tela inicial de entrada com identificador Matrix e senha (somente visual nesta versão);
- indicação explícita de que a conexão de mensagens ainda não está implementada.

**Não digite uma senha real na tela de demonstração.** A tela de entrada ainda não envia credenciais nem se conecta ao Matrix.

## Próximas etapas

1. Validar o build do APK de demonstração.
2. Integrar autenticação Matrix com armazenamento seguro de token e tratamento correto de erros.
3. Sincronizar e mostrar salas/conversas.
4. Abrir uma conversa e enviar/receber texto.
5. Testar no aparelho e corrigir antes de chamar o mensageiro de funcional.

O primeiro servidor planejado para teste é `matrix.org`. Isso usa a rede Matrix, não a rede Signal. O uso de `matrix.org` pode estar sujeito a limites e regras do provedor; revisar os termos atuais antes de uso prolongado.

## Compilação

O workflow GitHub Actions compila um APK de depuração e guarda o artefato por 7 dias. Não há chaves, tokens nem senha de assinatura no repositório.
