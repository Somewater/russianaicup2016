results_all = []
Dir['stat_*.txt'].each do |fn|
  name = /stat_(?<name>.+)\.txt/.match(fn)[:name]
  if File.exists?(fn)
    File.open(fn) do |f|
      results = f.read.split(',').map(&:to_i)
      mean = results.inject(&:+).to_f / results.size
      results_all << [name, mean, results]
    end
  end  
end
results_all.sort_by {|n, mean, _| mean }.each do |name, mean, results|
  puts "#{name}: #{mean.round(4)} by #{results.size} results"
end
