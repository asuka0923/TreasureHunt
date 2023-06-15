package plugin.treasurehunt.mapper.data;

import java.sql.Timestamp;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * プレイイヤーのスコア情報を扱うオブジェクト
 * DBのテーブルと連動する
 */
@Getter
@Setter
@NoArgsConstructor
public class PlayerScore {
    private int id;
    private Timestamp date;
    private int score;
    private int time;
    private String difficulty;

    public PlayerScore(int score, int time, String difficulty){
        this.score = score;
        this.time = time;
        this.difficulty = difficulty;
    }

}