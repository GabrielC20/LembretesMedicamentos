package facemp.edu.br.lembretesmedicamentos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TimePicker;
import android.widget.Toast;
import java.util.concurrent.TimeUnit;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private SimpleCursorAdapter adapter;
    private ListView listViewMedicamentos;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("ConfiguracoesApp", MODE_PRIVATE);

        Button btnAddMedicamento = findViewById(R.id.btnAddMedicamento);
        Button btnConfigurarNumero = findViewById(R.id.btnConfigurarNumero);
        listViewMedicamentos = findViewById(R.id.listViewMedicamentos);
        searchView = findViewById(R.id.buscarMedicamento);

        carregarMedicamentos();
        configurarLongClick();
        configurarBusca();

        btnAddMedicamento.setOnClickListener(v -> mostrarDialogAdicionarMedicamento());
        btnConfigurarNumero.setOnClickListener(v -> mostrarDialogConfigurarNumero());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.POST_NOTIFICATIONS
            }, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
    }

    private void carregarMedicamentos() {
        Cursor cursor = dbHelper.obterTodosMedicamentos();
        atualizarAdapter(cursor);
    }

    private void atualizarAdapter(Cursor cursor) {
        String[] from = new String[]{DatabaseHelper.COLUNA_NOME, DatabaseHelper.COLUNA_HORARIO};
        int[] to = new int[]{android.R.id.text1, android.R.id.text2};

        if (adapter == null) {
            adapter = new SimpleCursorAdapter(
                    this,
                    android.R.layout.simple_list_item_2,
                    cursor,
                    from,
                    to,
                    0);
            listViewMedicamentos.setAdapter(adapter);
        } else {
            adapter.changeCursor(cursor);
        }

        listViewMedicamentos.setOnItemClickListener((parent, view, position, id) -> {
            Cursor itemCursor = (Cursor) parent.getItemAtPosition(position);
            @SuppressLint("Range") String nome = itemCursor.getString(itemCursor.getColumnIndex(DatabaseHelper.COLUNA_NOME));
            @SuppressLint("Range") String horario = itemCursor.getString(itemCursor.getColumnIndex(DatabaseHelper.COLUNA_HORARIO));
            mostrarDialogConfirmacao(nome, horario);
        });
    }

    private void configurarBusca() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filtrarMedicamentos(newText);
                return true;
            }
        });

        searchView.setOnCloseListener(() -> {
            carregarMedicamentos();
            return false;
        });
    }

    private void filtrarMedicamentos(String textoBusca) {
        Cursor cursorFiltrado;
        if (textoBusca.isEmpty()) {
            cursorFiltrado = dbHelper.obterTodosMedicamentos();
        } else {
            cursorFiltrado = dbHelper.buscarMedicamentos(textoBusca);
        }
        atualizarAdapter(cursorFiltrado);
    }

    private void configurarLongClick() {
        listViewMedicamentos.setOnItemLongClickListener((parent, view, position, id) -> {
            Cursor cursor = (Cursor) adapter.getItem(position);
            @SuppressLint("Range") long medicamentoId = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUNA_ID));
            @SuppressLint("Range") String nome = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUNA_NOME));
            @SuppressLint("Range") String horario = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUNA_HORARIO));

            mostrarMenuContexto(medicamentoId, nome, horario);
            return true;
        });
    }

    private void mostrarMenuContexto(long id, String nome, String horario) {
        new AlertDialog.Builder(this)
                .setTitle(nome + " - " + horario)
                .setItems(new String[]{"Editar", "Excluir"}, (dialog, which) -> {
                    if (which == 0) {
                        mostrarDialogEditarMedicamento(id, nome, horario);
                    } else {
                        confirmarExclusao(id);
                    }
                })
                .show();
    }

    private void mostrarDialogAdicionarMedicamento() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Adicionar Medicamento");

        View view = getLayoutInflater().inflate(R.layout.add_medicamento, null);
        EditText etNome = view.findViewById(R.id.etNomeMedicamento);
        TimePicker timePicker = view.findViewById(R.id.timePickerHorario);

        builder.setView(view);
        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String nome = etNome.getText().toString();
            int hora = timePicker.getHour();
            int minuto = timePicker.getMinute();
            String horario = String.format("%02d:%02d", hora, minuto);

            long id = dbHelper.adicionarMedicamento(nome, horario);
            if (id != -1) {
                agendarNotificacao(nome, horario, (int) id);
                carregarMedicamentos();
                Toast.makeText(this, "Medicamento adicionado!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarDialogEditarMedicamento(long id, String nomeAtual, String horarioAtual) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar Medicamento");

        View view = getLayoutInflater().inflate(R.layout.add_medicamento, null);
        EditText etNome = view.findViewById(R.id.etNomeMedicamento);
        TimePicker timePicker = view.findViewById(R.id.timePickerHorario);

        etNome.setText(nomeAtual);
        String[] partes = horarioAtual.split(":");
        timePicker.setHour(Integer.parseInt(partes[0]));
        timePicker.setMinute(Integer.parseInt(partes[1]));

        builder.setView(view);
        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String novoNome = etNome.getText().toString();
            int hora = timePicker.getHour();
            int minuto = timePicker.getMinute();
            String novoHorario = String.format("%02d:%02d", hora, minuto);

            if (dbHelper.atualizarMedicamento(id, novoNome, novoHorario) > 0) {
                Toast.makeText(this, "Medicamento atualizado!", Toast.LENGTH_SHORT).show();
                cancelarAlarmeExistente((int) id);
                agendarNotificacao(novoNome, novoHorario, (int) id);
                carregarMedicamentos();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void confirmarExclusao(long id) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza que deseja excluir este medicamento?")
                .setPositiveButton("Excluir", (dialog, which) -> {
                    dbHelper.removerMedicamento(id);
                    Toast.makeText(this, "Medicamento excluído!", Toast.LENGTH_SHORT).show();
                    cancelarAlarmeExistente((int) id);
                    carregarMedicamentos();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogConfigurarNumero() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Configurar Número de Notificação");

        View view = getLayoutInflater().inflate(R.layout.config_numero, null);
        EditText etNumero = view.findViewById(R.id.etNumeroNotificacao);

        String numeroSalvo = sharedPreferences.getString("numero_notificacao", "");
        etNumero.setText(numeroSalvo);

        builder.setView(view);
        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String numero = etNumero.getText().toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("numero_notificacao", numero);
            editor.apply();
            Toast.makeText(this, "Número salvo com sucesso!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarDialogConfirmacao(String nome, String horario) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmação")
                .setMessage("Você tomou o medicamento " + nome + "?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    enviarSMSConfirmacao(nome, horario);
                    Toast.makeText(this, "Confirmação enviada!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Não", null)
                .show();
    }

    private void enviarSMSConfirmacao(String nome, String horarioProgramado) {
        String numero = sharedPreferences.getString("numero_notificacao", null);
        if (numero == null || numero.isEmpty()) {
            Toast.makeText(this, "Número de notificação não configurado", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Calendar agora = Calendar.getInstance();
            int horaAtual = agora.get(Calendar.HOUR_OF_DAY);
            int minutoAtual = agora.get(Calendar.MINUTE);
            String horarioReal = String.format("%02d:%02d", horaAtual, minutoAtual);

            String[] partes = horarioProgramado.split(":");
            int horaProgramada = Integer.parseInt(partes[0]);
            int minutoProgramado = Integer.parseInt(partes[1]);

            StringBuilder mensagem = new StringBuilder();
            mensagem.append("Medicamento ").append(nome).append("\n");
            mensagem.append("Programado para: ").append(horarioProgramado).append("\n");
            mensagem.append("Tomado às: ").append(horarioReal).append("\n");

            Calendar horarioPrevisto = Calendar.getInstance();
            horarioPrevisto.set(Calendar.HOUR_OF_DAY, horaProgramada);
            horarioPrevisto.set(Calendar.MINUTE, minutoProgramado);

            if (agora.after(horarioPrevisto)) {
                long diffMillis = agora.getTimeInMillis() - horarioPrevisto.getTimeInMillis();
                long diffHoras = TimeUnit.MILLISECONDS.toHours(diffMillis);
                long diffMinutos = TimeUnit.MILLISECONDS.toMinutes(diffMillis) % 60;

                if (diffHoras > 0) {
                    mensagem.append("(Atraso de ").append(diffHoras).append("h");
                    if (diffMinutos > 0) {
                        mensagem.append(" e ").append(diffMinutos).append("min");
                    }
                    mensagem.append(")");
                } else {
                    mensagem.append("(Atraso de ").append(diffMinutos).append("min)");
                }
            } else if (agora.before(horarioPrevisto)) {
                long diffMillis = horarioPrevisto.getTimeInMillis() - agora.getTimeInMillis();
                long diffHoras = TimeUnit.MILLISECONDS.toHours(diffMillis);
                long diffMinutos = TimeUnit.MILLISECONDS.toMinutes(diffMillis) % 60;

                if (diffHoras > 0) {
                    mensagem.append("(Adiantado em ").append(diffHoras).append("h");
                    if (diffMinutos > 0) {
                        mensagem.append(" e ").append(diffMinutos).append("min");
                    }
                    mensagem.append(")");
                } else {
                    mensagem.append("(Adiantado em ").append(diffMinutos).append("min)");
                }
            } else {
                mensagem.append("(No horário)");
            }

            if (horaAtual >= 6 && horaAtual < 22) {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(numero, null, mensagem.toString(), null, null);
            } else {
                Toast.makeText(this, "Confirmação registrada (SMS só é enviado entre 6h e 22h)", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void agendarNotificacao(String nome, String horario, int id) {
        String[] partes = horario.split(":");
        int hora = Integer.parseInt(partes[0]);
        int minuto = Integer.parseInt(partes[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hora);
        calendar.set(Calendar.MINUTE, minuto);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(this, ReceptorAlarme.class);
        intent.putExtra("nome_medicamento", nome);
        intent.putExtra("horario", horario);
        intent.putExtra("alarme_id", id);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
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

    private void cancelarAlarmeExistente(int id) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, ReceptorAlarme.class);


        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                id,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d("Alarme", "Alarme cancelado (ID: " + id + ")");
        }
    }
}