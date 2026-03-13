import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class RegexWorker {
    public static void main(String[] args) throws IOException {

        System.out.println("RegexWorker main 被执行");
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8)
        );
        String regex = reader.readLine();

        System.out.println("从 stdin 读取的正则: " + regex);

        System.out.println("参数数量: " + args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.println("args[" + i + "] = " + args[i]);
        }
        double[] pathInfo = RegexProcessor.processRegex(regex);

        // 用逗号分隔数值
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pathInfo.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(pathInfo[i]);
        }

        // 添加 testStrings1，使用特殊分隔符与前面的数值分开
        sb.append("||").append(RegexProcessor.testStrings1 == null ? "" : RegexProcessor.testStrings1);



        // 添加 testStrings1，使用特殊分隔符与前面的数值分开
        sb.append("||").append(RegexProcessor.mainPathTestStrings == null ? "" : RegexProcessor.mainPathTestStrings);


        // 最后一行输出，供主进程读取
        System.out.println(sb.toString());
    }
}
