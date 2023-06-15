package plugin.treasurehunt;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import plugin.treasurehunt.mapper.PlayerScoreMapper;
import plugin.treasurehunt.mapper.data.PlayerScore;

import java.io.InputStream;
import java.util.List;

/**
 * DB接続やそれに付随する登録や更新処理を行う
 */
public class PlayerScoreData {
    private SqlSessionFactory sqlSessionFactory;
    public PlayerScoreData() {
        try {
            InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
            this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public List<PlayerScore> selectList(){
        try (SqlSession session = sqlSessionFactory.openSession()) {
            PlayerScoreMapper mapper = session.getMapper(PlayerScoreMapper.class);
            return mapper.selectList();
        }
    }
    public void insert(PlayerScore playerScore){
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            PlayerScoreMapper mapper =session.getMapper(PlayerScoreMapper.class);
            mapper.insert(playerScore);

        }
    }
}
