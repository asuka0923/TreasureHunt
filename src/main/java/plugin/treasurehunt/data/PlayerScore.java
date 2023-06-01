package plugin.treasurehunt.data;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

/**
 * ゲームのスコア情報を扱う
 * 日時・難易度・スコア
 */
@Getter
@Setter
@Entity
@Table(name = "player_score")
public class PlayerScore {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;
  private int point;
  private int time;
  private String difficulty;
  public PlayerScore() {

  }
  public PlayerScore(int point, int time, String difficulty) {
    this.point = point;
    this.time = time;
    this.difficulty = difficulty;
  }

}
