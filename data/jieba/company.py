filename = 'industry.dic'
filename2 = 'industry.dict'

f = open(filename2, 'w')
for l in open(filename).readlines():
	l = l.strip()
	f.write(l + ' 3\n')
f.close()
