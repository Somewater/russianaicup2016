# `compile-java.bat`

raise "Need name" if ARGV.size < 2
name = ARGV.first.strip
socket_port = ARGV[1].to_i

N = if ARGV.size > 2
      ARGV[2].to_i
    else
      8
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

100.times do |_|
  `rm auto-runner/result-#{name}-*`

  poses = []
  runners = []
  strategies = []
  puts "Start local runners ..."
  N.times do |n|
    File.open("auto-runner/local-runner-console-#{name}-#{n}.properties", "w") do |config|
      config.write <<-FILE
  base-adapter-port=#{socket_port + n}
  results-file=result-#{name}-#{n}.txt
  log-file=
  FILE
    end
    runners << Thread.new(n) do |n|
      `cd auto-runner && java -Xms512m -Xmx1G -jar "local-runner.jar" local-runner-console-#{name}-#{n}.properties local-runner-console.default.properties`
    end
  end

  sleep(5)
  puts "Start strategies ..."

  N.times do |n|
    strategies << Thread.new(n) do |n|
      `java -jar #{name}.jar 127.0.0.1 #{socket_port + n} 0000000000000000`
    end
  end


  strategies.map{|r| r.join; puts "Strategy completed" }
  runners.map{|r| r.join; puts "Runner completed" }


  N.times do |n|
    v = parse_result("auto-runner/result-#{name}-#{n}.txt")
    if v
      poses << v
    end
  end

  mean_pos = poses.inject(&:+).to_f / poses.size
  puts "\nMean pos: #{mean_pos} (by #{poses.size} vals)"

  exist = File.exist?("stat_#{name}.txt")
  File.open("stat_#{name}.txt", 'a') do |out|
    out.write(',') if (exist)
    out.write(poses.map(&:to_s).join(","))
  end
end