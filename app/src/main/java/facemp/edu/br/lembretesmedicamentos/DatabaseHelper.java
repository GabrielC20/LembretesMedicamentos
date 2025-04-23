package facemp.edu.br.lembretesmedicamentos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String NOME_BANCO = "medicamentos.db";
    private static final int VERSAO_BANCO = 2;

    public static final String TABELA_MEDICAMENTOS = "medicamentos";
    public static final String COLUNA_ID = "_id";
    public static final String COLUNA_NOME = "nome";
    public static final String COLUNA_HORARIO = "horario";

    private static final String CRIAR_TABELA =
            "CREATE TABLE " + TABELA_MEDICAMENTOS + " (" +
                    COLUNA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUNA_NOME + " TEXT NOT NULL, " +
                    COLUNA_HORARIO + " TEXT NOT NULL);";

    public DatabaseHelper(Context context) {
        super(context, NOME_BANCO, null, VERSAO_BANCO);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CRIAR_TABELA);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int versaoAntiga, int versaoNova) {
        db.execSQL("DROP TABLE IF EXISTS " + TABELA_MEDICAMENTOS);
        onCreate(db);
    }

    public long adicionarMedicamento(String nome, String horario) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(COLUNA_NOME, nome);
        valores.put(COLUNA_HORARIO, horario);
        long id = db.insert(TABELA_MEDICAMENTOS, null, valores);
        db.close();
        return id;
    }

    public Cursor buscarMedicamentos(String termoBusca) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABELA_MEDICAMENTOS +
                " WHERE " + COLUNA_NOME + " LIKE ?";
        String[] args = {"%" + termoBusca + "%"};
        return db.rawQuery(query, args);
    }

    public Cursor obterTodosMedicamentos() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABELA_MEDICAMENTOS, null, null, null, null, null, COLUNA_HORARIO + " ASC");
    }

    public int atualizarMedicamento(long id, String novoNome, String novoHorario) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(COLUNA_NOME, novoNome);
        valores.put(COLUNA_HORARIO, novoHorario);
        return db.update(TABELA_MEDICAMENTOS, valores, COLUNA_ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    public void removerMedicamento(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABELA_MEDICAMENTOS, COLUNA_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
    }
}
