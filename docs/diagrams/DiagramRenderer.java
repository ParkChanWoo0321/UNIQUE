import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/**
 * Dependency-free portfolio diagrams. Run with JDK 17 and a Korean font.
 * PNG and editable SVG share the same coordinates and text.
 * This is documentation tooling, not part of the application build.
 */
public class DiagramRenderer {
    static final String INK="#172B46", MUTED="#526780", LINE="#CED9E5";
    static final String BLUE="#285BC7", TEAL="#087F79", GOLD="#A16613";
    static final String PALE="#F4F7FB", WHITE="#FFFFFF", GREEN="#EAF7F4";
    static final String FONT = "Malgun Gothic";
    static Path out;

    static class Canvas {
        final int w,h;
        final BufferedImage image;
        final Graphics2D g;
        final StringBuilder svg=new StringBuilder();
        Canvas(int w,int h,String title,String description) {
            this.w=w; this.h=h;
            image=new BufferedImage(w*2,h*2,BufferedImage.TYPE_INT_RGB);
            g=image.createGraphics(); g.scale(2,2);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(w)
               .append("\" height=\"").append(h).append("\" viewBox=\"0 0 ").append(w).append(" ").append(h)
               .append("\" role=\"img\"><title>").append(esc(title)).append("</title><desc>")
               .append(esc(description)).append("</desc>");
            rect(0,0,w,h,0,WHITE,null);
        }
        void rect(double x,double y,double w,double h,double r,String fill,String stroke) {
            Shape s=new RoundRectangle2D.Double(x,y,w,h,r*2,r*2);
            if(fill!=null){g.setColor(Color.decode(fill));g.fill(s);}
            if(stroke!=null){g.setColor(Color.decode(stroke));g.setStroke(new BasicStroke(1.5f));g.draw(s);}
            svg.append(String.format(Locale.ROOT,
                "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" rx=\"%.1f\" fill=\"%s\" stroke=\"%s\" stroke-width=\"1.5\"/>",
                x,y,w,h,r,fill==null?"none":fill,stroke==null?"none":stroke));
        }
        void text(double x,double y,String s,int size,String color,boolean bold) {
            Font f=new Font(FONT,bold?Font.BOLD:Font.PLAIN,size);
            g.setFont(f); g.setColor(Color.decode(color));
            if(x+g.getFontMetrics().stringWidth(s)>w-20)
                throw new IllegalArgumentException("Text outside canvas: "+s);
            g.drawString(s,(float)x,(float)y);
            svg.append(String.format(Locale.ROOT,
                "<text x=\"%.1f\" y=\"%.1f\" font-family=\"Malgun Gothic, Noto Sans KR, sans-serif\" font-size=\"%d\" font-weight=\"%s\" fill=\"%s\">%s</text>",
                x,y,size,bold?"700":"400",color,esc(s)));
        }
        double width(String s,int size,boolean bold) {
            return g.getFontMetrics(new Font(FONT,bold?Font.BOLD:Font.PLAIN,size)).stringWidth(s);
        }
        void center(double x,double y,String s,int size,String color,boolean bold) {
            text(x-width(s,size,bold)/2,y,s,size,color,bold);
        }
        void path(String color,boolean dash,boolean arrow,double... p) {
            Path2D path=new Path2D.Double();path.moveTo(p[0],p[1]);
            StringBuilder d=new StringBuilder("M "+p[0]+" "+p[1]);
            for(int i=2;i<p.length;i+=2){path.lineTo(p[i],p[i+1]);d.append(" L ").append(p[i]).append(" ").append(p[i+1]);}
            g.setColor(Color.decode(color));
            g.setStroke(dash?new BasicStroke(2,0,0,10,new float[]{7,6},0):new BasicStroke(2));
            g.draw(path);
            svg.append("<path d=\"").append(d).append("\" fill=\"none\" stroke=\"").append(color)
                .append("\" stroke-width=\"2\"").append(dash?" stroke-dasharray=\"7 6\"":"").append("/>");
            if(arrow) {
                int n=p.length; double x=p[n-2],y=p[n-1],a=Math.atan2(y-p[n-3],x-p[n-4]);
                double x1=x-11*Math.cos(a-.42),y1=y-11*Math.sin(a-.42);
                double x2=x-11*Math.cos(a+.42),y2=y-11*Math.sin(a+.42);
                path(color,false,false,x1,y1,x,y,x2,y2);
            }
        }
        void label(double cx,double y,String s,int size,String color) {
            double tw=width(s,size,true);
            rect(cx-tw/2-10,y-size-4,tw+20,size+12,6,WHITE,null);
            center(cx,y,s,size,color,true);
        }
        void header(String index,String title,String subtitle) {
            text(50,48,"또롱모  /  BACKEND ENGINEERING",17,TEAL,true);
            text(50,99,title,36,INK,true);
            text(50,136,subtitle,20,MUTED,false);
            text(w-110,100,index,44,LINE,true);
        }
        void footer(String s) {
            path(LINE,false,false,50,h-62,w-50,h-62);
            text(50,h-28,s,16,MUTED,false);
        }
        void card(int x,int y,int w,int h,String title,String sub,String color) {
            rect(x,y,w,h,14,WHITE,LINE);
            rect(x,y,6,h,3,color,null);
            text(x+22,y+33,title,23,INK,true);
            if(sub!=null)text(x+22,y+62,sub,18,MUTED,false);
        }
        void save(String name)throws Exception {
            g.dispose();svg.append("</svg>\n");
            Files.writeString(out.resolve(name+".svg"),svg,StandardCharsets.UTF_8);
            ImageIO.write(image,"png",out.resolve(name+".png").toFile());
            System.out.println(name+": "+w+"x"+h+" SVG / "+w*2+"x"+h*2+" PNG");
        }
    }
    static String esc(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}

    static void architecture()throws Exception {
        Canvas c=new Canvas(1440,1120,"또롱모 backend architecture","Single Spring Boot application, domain layers and Cloud SQL connection.");
        c.header("01","백엔드 시스템 아키텍처","단일 Spring Boot 애플리케이션 · 도메인별 책임 분리 · 관계형 데이터 저장");
        c.card(510,175,420,105,"Browser / API client","REST 요청 · participant_token 쿠키",BLUE);
        c.rect(60,175,390,105,12,PALE,null);
        c.text(80,207,"참가자 식별",18,TEAL,true);
        c.text(80,239,"SecureRandom 32 bytes → HttpOnly cookie",17,INK,false);
        c.text(80,264,"초기화 응답에서 Set-Cookie 발급",17,MUTED,false);
        c.rect(990,175,390,105,12,PALE,null);
        c.text(1010,207,"환경별 연동 설정",18,TEAL,true);
        c.text(1010,239,"CORS 허용 출처 + credentials",18,INK,false);
        c.text(1010,264,"Secure / SameSite는 환경변수로 설정",17,MUTED,false);
        c.path(BLUE,false,true,720,280,720,345);
        c.label(720,319,"HTTPS · JSON",17,BLUE);
        c.rect(60,345,1320,555,20,PALE,LINE);
        c.text(85,381,"Google Cloud Run  /  Spring Boot 4.1.1 · Java 17",23,INK,true);
        c.text(85,412,"Web MVC · Bean Validation · 공통 예외 응답",18,MUTED,false);
        int[] xs={100,540,980};
        String[] controllers={"ParticipantController","GameController","OperationController"};
        String[] routes={"/api/participants/**","/api/game/**","/api/operations/**"};
        String[] services={"ParticipantService","GameService","OperationService"};
        String[] jobs={"식별 · 날짜별 초기화 · 내역 조회","최초·추가 응모 · 상태·결과 조회","보너스 · 결과 확정 · 통합 재계산"};
        for(int i=0;i<3;i++){
            c.card(xs[i],435,360,85,controllers[i],routes[i],BLUE);
            c.path(BLUE,false,true,xs[i]+180,520,xs[i]+180,552);
            c.card(xs[i],552,360,91,services[i],jobs[i],TEAL);
            c.path(TEAL,false,true,xs[i]+180,643,xs[i]+180,684);
        }
        c.rect(100,684,1240,88,12,WHITE,LINE);
        c.text(122,716,"Spring Data JPA / Hibernate",23,INK,true);
        c.text(122,747,"Participant · DailyParticipation · Submission · BonusEvent · GameResult Repository",20,MUTED,false);
        c.path(TEAL,false,true,720,772,720,800);
        c.rect(100,800,1240,70,12,GREEN,null);
        c.center(720,830,"HikariCP  →  MySQL JDBC driver  +  Cloud SQL Connector",22,TEAL,true);
        c.center(720,855,"DB_URL · DB_USERNAME · DB_PASSWORD / DB_POOL_MAX_SIZE · DB_POOL_MIN_IDLE",16,MUTED,false);
        c.path(TEAL,false,true,720,900,720,946);
        c.card(390,946,660,92,"Cloud SQL for MySQL","참가자 · 날짜별 참여 · 응모 · 보너스 이력 · 확정 결과",TEAL);
        c.footer("배포 구성과 코드 의존성을 표현한 그림입니다. 운영 API 인증·인가는 현재 미구현이며 현재 가동 상태를 뜻하지 않습니다.");
        c.save("architecture");
    }

    record Row(String key,String name,String type,boolean legacy) {
        Row(String key,String name,String type){this(key,name,type,false);}
    }
    static int table(Canvas c,int x,int y,String name,String role,String color,Row[] rows,String... notes) {
        int w=480,h=82+rows.length*29+notes.length*24+17;
        c.rect(x,y,w,h,14,WHITE,LINE);
        c.rect(x,y,w,78,14,color,null);
        c.text(x+20,y+32,name,25,WHITE,true);
        c.text(x+20,y+60,role,17,WHITE,false);
        int cy=y+104;
        for(Row r:rows){
            if(r.legacy)c.rect(x+1,cy-21,w-2,29,0,PALE,null);
            c.text(x+16,cy,r.key,14,r.legacy?MUTED:color,true);
            c.text(x+94,cy,r.name,17,r.legacy?MUTED:INK,false);
            c.text(x+411,cy,r.type,14,MUTED,false);
            cy+=29;
        }
        if(notes.length>0){
            c.path(LINE,false,false,x+16,cy-10,x+w-16,cy-10);
            cy+=14;
            for(String n:notes){c.text(x+16,cy,n,15,MUTED,false);cy+=24;}
        }
        return y+h;
    }
    static void erd()throws Exception {
        Canvas c=new Canvas(1680,1450,"또롱모 entity relationship diagram","Five physical tables, JPA foreign keys, cardinalities and unique constraints.");
        c.header("02","데이터베이스 ERD","JPA 매핑 기준 · 물리 테이블명 · 실제 외래키 관계와 복합 UNIQUE 제약");
        table(c,600,180,"participants","브라우저 참가자 식별",BLUE,new Row[]{
            new Row("PK","id","Long"),new Row("UQ","participant_token","String"),
            new Row("LEGACY","phone_number","String",true),
            new Row("LEGACY","base_attempts","int",true),new Row("LEGACY","bonus_attempts","int",true),
            new Row("LEGACY","used_attempts","int",true),new Row("LEGACY","participation_completed","boolean",true),
            new Row("LEGACY","privacy_agreed","boolean",true),new Row("LEGACY","privacy_agreed_at","Instant",true),
            new Row("","created_at","Instant"),new Row("","updated_at","Instant"),new Row("VERSION","version","long")
        },"LEGACY: 이전 스키마 호환용 필드");
        table(c,50,290,"daily_participations","날짜별 응모 상태의 기준 데이터",TEAL,new Row[]{
            new Row("PK","id","Long"),new Row("FK","participant_id","Long"),new Row("","event_date","Date"),
            new Row("","base_attempts","int"),new Row("","bonus_attempts","int"),new Row("","used_attempts","int"),
            new Row("","participation_completed","boolean"),new Row("","phone_number","String"),
            new Row("","privacy_agreed","boolean"),new Row("","privacy_agreed_at","Instant"),
            new Row("","created_at","Instant"),new Row("","updated_at","Instant"),new Row("VERSION","version","long")
        },"UQ (participant_id, event_date)","UQ (event_date, phone_number)");
        table(c,1150,290,"submissions","숫자 1개당 응모 1행",TEAL,new Row[]{
            new Row("PK","id","Long"),new Row("FK","participant_id","Long"),new Row("","number_value","int"),
            new Row("","event_date","Date"),new Row("","submitted_at","Instant")
        },"IDX (event_date, number_value)","IDX (participant_id, event_date)","event_date: 기존 데이터 호환을 위해 nullable");
        table(c,1150,850,"game_results","날짜별 확정 결과",BLUE,new Row[]{
            new Row("PK","id","Long"),new Row("UQ","result_key","String"),new Row("UQ","event_date","Date"),
            new Row("","winning_number","Integer"),new Row("FK / UQ","winning_submission_id","Long"),
            new Row("FK","winner_participant_id","Long"),new Row("","calculated_at","Instant"),new Row("","created_at","Instant")
        },"당첨자가 없으면 당첨 번호와 두 FK는 null","통합 재계산은 이 테이블에 저장하지 않음");
        table(c,50,960,"bonus_events","날짜·단계별 실행 이력 / 독립 테이블",GOLD,new Row[]{
            new Row("PK","id","Long"),new Row("UQ","event_key","String"),new Row("","event_date","Date"),
            new Row("","stage","Integer"),new Row("","bonus_attempts","int"),
            new Row("","target_participant_count","int"),new Row("","executed","boolean"),
            new Row("","executed_at","Instant"),new Row("","created_at","Instant")
        },"UQ (event_date, stage)");
        // End labels specify multiplicity at the corresponding entity.
        c.path(TEAL,false,false,600,220,562,220,562,330,530,330);
        c.label(578,208,"1",17,TEAL); c.label(558,365,"0..N",17,TEAL);
        c.path(TEAL,false,false,1080,220,1115,220,1115,330,1150,330);
        c.label(1102,208,"1",17,TEAL); c.label(1118,365,"0..N",17,TEAL);
        c.path(BLUE,false,false,840,651,840,950,1150,950);
        c.label(840,692,"0..1",18,BLUE);c.label(1101,938,"0..N",18,BLUE);
        c.label(840,808,"당첨 참가자",18,BLUE);
        c.path(BLUE,false,false,1390,606,1390,850);
        c.label(1390,646,"0..1",18,BLUE);c.label(1390,829,"0..1",18,BLUE);
        c.label(1390,730,"당첨 응모",18,BLUE);
        c.rect(600,1080,480,270,14,PALE,null);
        c.text(622,1117,"관계를 읽는 기준",21,INK,true);
        c.text(622,1154,"실선: JPA에 정의된 외래키 관계",18,MUTED,false);
        c.text(622,1186,"1 / 0..1 / 0..N: 관계 끝의 개체 수",18,MUTED,false);
        c.text(622,1218,"PK 기본키 · FK 외래키 · UQ 유일 제약",17,MUTED,false);
        c.text(622,1250,"오른쪽 타입은 Java 타입 기준",17,MUTED,false);
        c.text(622,1282,"보너스는 행사일로 조회하며 FK 없음",17,MUTED,false);
        c.text(622,1314,"참여 정보와 응모 사이에 직접 FK 없음",17,MUTED,false);
        c.footer("엔티티 5개의 전체 필드를 표시했습니다. Date = LocalDate / VERSION = @Version / 인덱스는 핵심 조회 경로만 표시.");
        c.save("erd");
    }

    static void sequence()throws Exception {
        Canvas c=new Canvas(1540,1510,"또롱모 initial entry sequence","Validation, pessimistic locks, atomic updates, flush and transaction completion.");
        c.header("03","최초 응모 시퀀스","POST /api/game/entries · 날짜별 초기화가 완료된 참가자 · 정상 처리와 실패 경계");
        int client=145,controller=465,service=855,db=1350;
        int[] centers={client,controller,service,db};
        String[] titles={"Client","GameController","GameService + Spring TX","Repository / DB"};
        int[] widths={190,260,350,270};
        for(int i=0;i<4;i++){
            int x=centers[i]-widths[i]/2;
            c.rect(x,180,widths[i],66,12,i>=2?GREEN:PALE,LINE);
            c.center(centers[i],219,titles[i],21,INK,true);
        }
        c.rect(665,370,815,850,14,"#F2FAF8",LINE);
        c.text(685,401,"@Transactional  /  성공 시 함께 커밋 · 예외 시 롤백",19,TEAL,true);
        for(int x:centers)c.path(LINE,true,false,x,247,x,1305);
        c.path(BLUE,false,true,client,282,controller,282);
        c.label(305,271,"쿠키 + JSON 요청",18,BLUE);
        c.rect(330,300,270,43,8,PALE,null);
        c.center(controller,328,"@Valid 요청 DTO 검증",18,INK,true);
        c.path(BLUE,false,true,controller,356,service,356);
        c.label(662,345,"createEntry(token, request)",17,BLUE);
        c.rect(715,421,285,43,8,WHITE,LINE);
        c.center(service,449,"행사일 검증 · 입력 날짜 확정",17,INK,false);
        c.path(TEAL,false,true,service,505,db,505);
        c.label(1100,494,"결과 존재 여부 조회 · 응모 가능 상태 확인",17,TEAL);
        c.rect(705,537,300,58,8,WHITE,LINE);
        c.center(service,562,"동의 · 숫자 3개 · 범위 · 서로 다른 값",16,INK,false);
        c.center(service,584,"validateInitialEntry()",16,MUTED,false);
        c.path(TEAL,false,true,service,637,db,637);
        c.label(1100,625,"① 참가자 조회 + PESSIMISTIC_WRITE",18,TEAL);
        c.path(TEAL,false,true,service,711,db,711);
        c.label(1100,699,"② 날짜별 참여 조회 + PESSIMISTIC_WRITE",18,TEAL);
        c.rect(710,741,290,58,8,WHITE,LINE);
        c.center(service,766,"최초 응모 여부 확인",17,INK,false);
        c.center(service,789,"전화번호 정규화",17,INK,false);
        c.path(TEAL,false,true,service,837,db,837);
        c.label(1100,825,"같은 날짜의 전화번호 중복 조회",18,TEAL);
        c.rect(698,866,314,62,8,WHITE,LINE);
        c.center(service,891,"전화번호·동의 시각 저장",17,INK,false);
        c.center(service,915,"usedAttempts += 3 / 참여 완료",17,INK,false);
        c.path(TEAL,false,true,service,968,db,968);
        c.label(1100,956,"DailyParticipationRepository.saveAndFlush()",16,TEAL);
        c.path(TEAL,false,true,service,1042,db,1042);
        c.label(1100,1030,"SubmissionRepository.saveAllAndFlush() · 3행",16,TEAL);
        c.rect(700,1071,310,43,8,WHITE,LINE);
        c.center(service,1099,"CreateEntryResponse 구성",18,INK,false);
        c.path(TEAL,false,true,service,1160,db,1160);
        c.label(1100,1148,"Spring TX: COMMIT · 잠금 해제",18,TEAL);
        c.path(BLUE,true,true,service,1246,controller,1246);
        c.label(663,1235,"커밋 성공 후 DTO 반환",18,BLUE);
        c.path(BLUE,true,true,controller,1296,client,1296);
        c.label(305,1285,"200 OK · 응모 내역·남은 기회",17,BLUE);
        c.rect(60,1330,1420,99,12,PALE,null);
        c.text(82,1361,"실패 처리",19,INK,true);
        c.text(205,1361,"@Valid 실패 → 서비스 진입 전 400 / 비즈니스 예외 → 트랜잭션 롤백 후 400·404·409",18,MUTED,false);
        c.text(205,1394,"전화번호 UNIQUE 충돌 → DUPLICATE_PHONE_NUMBER (409). flush는 커밋이 아니며 잠금은 트랜잭션 종료까지 유지.",17,MUTED,false);
        c.footer("GameController.createEntry → GameService.createEntry 기준. Repository/DB는 한 레인으로 묶었으며 행사 전체의 종료 잠금은 없습니다.");
        c.save("entry-sequence");
    }
    public static void main(String[] args)throws Exception {
        out=Path.of(args.length>0?args[0]:"docs/diagrams");Files.createDirectories(out);
        architecture();erd();sequence();
    }
}
