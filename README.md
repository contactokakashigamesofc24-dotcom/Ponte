# Ponte 0.3.0 — confirmar dispositivo e mensagens seguras

Após login/restauração, o Ponte verifica o estado E2EE do SDK Matrix. Para dispositivo não confirmado, mostra a etapa “Continue sua identidade”:
- Confirmação com outro dispositivo por SAS Matrix real, incluindo comparação de emojis/números. A aprovação só acontece após o usuário dizer que os códigos coincidem.
- Recuperação pelo SDK usando a chave de recuperação Matrix. A chave é solicitada em campo protegido e não é salva pelo app.
- “Não consigo confirmar” mostra consequências e não faz reset de identidade automaticamente. “Agora não” permite abrir a lista de salas sem afirmar que a E2EE foi configurada.

O build compila e tem assinatura permanente, mas isso não substitui teste Matrix/E2EE no telefone.
