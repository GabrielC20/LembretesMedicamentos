package facemp.edu.br.lembretesmedicamentos;

import android.app.AlarmManager;
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
import java.util.Calendar;

public class ReceptorAlarme extends BroadcastReceiver {
    private static final String ID_CANAL = "canal_lembrete_medicamento";
    private static final int PENDING_INTENT_REQUEST_CODE = 0;

    @Override
    public void onReceive(Context context, Intent intent) {

        String nomeMedicamento = intent.getStringExtra("nome_medicamento");
        String dosagem = intent.getStringExtra("dosagem");
        String horario = intent.getStringExtra("horario");
        int alarmeId = intent.getIntExtra("alarme_id", 0);


        exibirNotificacao(context, nomeMedicamento, horario);


        reagendarAlarme(context, nomeMedicamento, horario, alarmeId);
    }

    private void exibirNotificacao(Context context, String nomeMedicamento, String horario) {

        criarCanalNotificacao(context);


        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                PENDING_INTENT_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notificacao = new NotificationCompat.Builder(context, ID_CANAL)
                .setContentTitle("Hora do Medicamento!")
                .setContentText("Está na hora de tomar " + nomeMedicamento + " (" + dosagem + ") às " + horario)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build();

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(generateNotificationId(), notificacao);
    }

    private void reagendarAlarme(Context context, String nomeMedicamento, String horario, int alarmeId) {

        Calendar calendar = parseHorario(horario);
        calendar.add(Calendar.DAY_OF_YEAR, 1);


        Intent intent = new Intent(context, ReceptorAlarme.class);
        intent.putExtra("nome_medicamento", nomeMedicamento);
        intent.putExtra("horario", horario);
        intent.putExtra("alarme_id", alarmeId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmeId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );


        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
        }
    }

    private Calendar parseHorario(String horario) {
        String[] partes = horario.split(":");
        int hora = Integer.parseInt(partes[0]);
        int minuto = Integer.parseInt(partes[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hora);
        calendar.set(Calendar.MINUTE, minuto);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    private void criarCanalNotificacao(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    ID_CANAL,
                    "Lembretes de Medicação",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription("Canal para notificações de lembrete de medicação");
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private int generateNotificationId() {
        return (int) System.currentTimeMillis();
    }
}