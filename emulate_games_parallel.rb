# `compile-java.bat`

N = if ARGV.size > 0
      ARGV[0].to_i
    else
      5
    end

def parse_result(filepath)
  pos = nil
  if File.exist?(filepath)
    File.open(filepath) do |file|
      lines = file.each_line.to_a.map{|l| l.strip }
      pos = lines.drop(2).index{|l| l.start_with?("1 ") } # 0 - best, 9 - worst
      puts "  pos = #{pos}"
    end
    return pos
  else
    nil
  end
end

`rm auto-runner/result-*`

poses = []
runners = []
strategies = []
puts "Start local runners ..."
N.times do |n|
  File.open("auto-runner/local-runner-console-#{n}.properties", "w") do |config|
    config.write <<-FILE
base-adapter-port=#{41001 + n}
results-file=result-#{n}.txt
log-file=
FILE
  end
  runners << Thread.new(n) do |n|
    `cd auto-runner && java -Xms512m -Xmx1G -jar "local-runner.jar" local-runner-console-#{n}.properties local-runner-console.default.properties`
  end
end

sleep(5)
puts "Start strategies ..."

N.times do |n|
  strategies << Thread.new(n) do |n|
    `java -jar java-cgdk.jar 127.0.0.1 #{41001 + n} 0000000000000000`
  end
end


runners.map{|r| r.join; puts "Runner completed" }
strategies.map(&:join)

N.times do |n|
  v = parse_result("auto-runner/result-#{n}.txt")
  if v
    poses << v
  end
end

mean_pos = poses.inject(&:+).to_f / poses.size
puts "\nMean pos: #{mean_pos} (by #{poses.size} vals)"