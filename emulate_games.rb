# `compile-java.bat`

N = 10

poses = []
N.times do |n|
  t1 = Thread.new do
    `cd local-runner && local-runner-console.bat`
  end
  sleep(0.3)
  t2 = Thread.new do
    `java -jar java-cgdk.jar 127.0.0.1 41001 0000000000000000`
  end
  t1.join()
  t2.join()

  File.open('local-runner/result.txt') do |file|
    lines = file.each_line.to_a.map{|l| l.strip }
    pos = lines.drop(2).index{|l| l.start_with?("1 ") } # 0 - best, 9 - worst
    puts "  #{n}) pos = #{pos}"
    poses << pos
  end
end

mean_pos = poses.inject(&:+).to_f / poses.size
puts "Mean pos: #{mean_pos}"