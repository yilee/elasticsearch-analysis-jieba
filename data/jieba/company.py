filename = 'university.dict'
filename2 = 'university_2.dict'

f = open(filename2, 'w')
for l in open(filename).readlines():
	l = l.strip()
	l = l.replace(' ','')
	f.write(l + ' 3\n')
f.close()
