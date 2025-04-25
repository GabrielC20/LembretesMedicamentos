package facemp.edu.br.lembretesmedicamentos;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class ReceptorAlarme extends BroadcastReceiver {
    private static final String ID_CANAL = "canal_lembrete_medicacao";

    @Override
    public void onReceive(Context contexto, Intent intent) {
        String nomeMedicamento = intent.getStringExtra("nome_medicamento");
        String dosagem = intent.getStringExtra("dosagem");
        String horario = intent.getStringExtra("horario");

        criarCanalNotificacao(contexto);

        Intent intentPrincipal = new Intent(contexto, MainActivity.class);
        intentPrincipal.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent intencaoPendente = PendingIntent.getActivity(
                contexto,
                0,
                intentPrincipal,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notificacao = new NotificationCompat.Builder(contexto, ID_CANAL)
                .setContentTitle("Hora do Medicamento!")
                .setContentText("Está na hora de tomar " + nomeMedicamento + " (" + dosagem + ") às " + horario)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(intencaoPendente)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        NotificationManager gerenciadorNotificacoes =
                (NotificationManager) contexto.getSystemService(Context.NOTIFICATION_SERVICE);

        int idNotificacao = (int) System.currentTimeMillis();
        gerenciadorNotificacoes.notify(idNotificacao, notificacao);
    }

    private void criarCanalNotificacao(Context contexto) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence nome = "Lembretes de Medicação";
            String descricao = "Canal para notificações de lembrete de medicação";
            int importancia = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel canal = new NotificationChannel(ID_CANAL, nome, importancia);
            canal.setDescription(descricao);
            canal.enableLights(true);
            canal.setLightColor(Color.BLUE);
            canal.enableVibration(true);

            NotificationManager gerenciadorNotificacoes =
                    contexto.getSystemService(NotificationManager.class);
            gerenciadorNotificacoes.createNotificationChannel(canal);
        }
    }
}