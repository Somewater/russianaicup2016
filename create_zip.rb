require 'zip'

dirs = [
    Dir["./src/main/java/*.java"],
    Dir["./astar/*.java"]
 ]


i = 1
while File.exists("v#{i}.zip") do
  i += 1
end

Zip::File.open("v#{i}.zip", Zip::File::CREATE) do |zip|
  zipfile.get_output_stream("first.txt") { |f| f.puts "Hello from ZipFile" }

  dirs.each do |files|
    files.each do |filepath|
      filename = File.basename(filepath)
      File.open(filepath) do |file|
        zip.get_output_stream(filename) { |f| f.write file }
      end
    end
  end
end