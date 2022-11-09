package main

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"math"
	"math/rand"
	"regexp"
	"sort"
	"strings"
	"time"

	log "github.com/schollz/logger"
)

type Data struct {
	Track        []Track            `json:"track"`
	Chain        []string           `json:"chain"`
	Pulses       int                `json:"pulses"`
	Patterns     map[string]Pattern `json:"patterns"`
	Meta         []string           `json:"meta"`
	PatternChain []string           `json:"pattern_chain"`
	FullText     string             `json:"fulltext"`
}
type Track struct {
	Mods     []string `json:"mods,omitempty"`
	Duration int      `json:"duration"`
	Start    int      `json:"start"`
	M        int      `json:"m"`
}
type Parsed struct {
	Track     []Track    `json:"track,omitempty"`
	Positions []Position `json:"positions,omitempty"`
	Pulses    int        `json:pulses,omitempty"`
}
type ParsedNote struct {
	M int    `json:"m,omitempty"`
	N string `json:"n,omitempty"`
}
type Position struct {
	El          string       `json:"el"`
	Start       int          `json:"start"`
	ParsedNotes []ParsedNote `json:"parsed"`
	Mods        []string     `json:"mods"`
	Stop        int          `json:"stop"`
	Line        int          `json:"line"`
}
type Pattern struct {
	Parsed  Parsed `json:"parsed"`
	Pattern string `json:"pattern"`
	Text    string `json:"text"`
}

var notes = []string{"c", "c#", "d", "d#", "e", "f", "f#", "g", "g#", "a", "a#", "b"}

func rotate(nums []float64, k int) []float64 {
	if len(nums) == 0 {
		return nums
	}
	for {
		if k == 0 {
			return nums
		} else if k < 0 {
			k = k + len(nums)
		} else {
			break
		}
	}

	r := len(nums) - k%len(nums)
	nums = append(nums[r:], nums[:r]...)

	return nums
}

func noteDiff(num1, num2 float64) (diff float64) {
	diff = 1000000.0
	for _, add1 := range []float64{-12, 0, 12} {
		for _, add2 := range []float64{-12, 0, 12} {
			d := math.Abs((num1 + add1) - (num2 + add2))
			if d < diff {
				diff = d
			}
		}
	}
	return
}

func sumRowDiffs(matrix [][]float64) (sum float64) {
	for row := range matrix {
		if row == 0 {
			continue
		}
		for col := range matrix[row] {
			sum += noteDiff(matrix[row][col], matrix[row-1][col])
		}
	}
	return
}

func printMatrix(m [][]float64) {
	for row := range m {
		for col := range m[row] {
			fmt.Printf("%2.0f\t", m[row][col])
		}
		fmt.Print("\n")
	}
}

func printMatrixS(m [][]string) {
	for row := range m {
		for col := range m[row] {
			fmt.Printf("%s\t", m[row][col])
		}
		fmt.Print("\n")
	}
}

func numToNote(num float64) string {
	return notes[numTo12(num)]
}

func numTo12(num float64) int {
	for {
		if num >= 0 {
			break
		}
		num += 12
	}
	return int(math.Mod(num, 12))
}

func copyMatrix(m [][]float64) (m2 [][]float64) {
	m2 = make([][]float64, len(m))
	for i := range m {
		m2[i] = make([]float64, len(m[i]))
		copy(m2[i], m[i])
	}
	return
}

func rearrangeMatrix(m [][]float64) ([][]float64, [][]float64) {
	type Result struct {
		matrix  [][]float64
		rowDiff float64
	}
	numTries := 1000
	results := make([]Result, numTries)
	rand.Seed(int64(time.Now().Nanosecond()))
	for i := 0; i < numTries; i++ {
		for j, v := range m {
			m[j] = rotate(v, rand.Intn(len(v)))
		}
		results[i] = Result{copyMatrix(m), sumRowDiffs(m)}
	}

	sort.Slice(results, func(i, j int) bool {
		return results[i].rowDiff < results[j].rowDiff
	})
	log.Tracef("results[0]: %+v", results[0])
	log.Tracef("results[end]: %+v", results[len(results)-1])
	return results[0].matrix, results[len(results)-1].matrix
}

func assignNotes(m [][]float64) (m2 [][]string) {
	m2 = make([][]string, len(m))
	octaves := []int{0, 1, 2, 3, 4, 5}
	for row := range m {
		m2[row] = make([]string, len(m[row]))
		for col := range m[row] {
			m2[row][col] = fmt.Sprintf("%s%d", numToNote(m[row][col]), octaves[col]) //+rand.Intn(2))
		}
	}
	return
}

func countNotes(m [][]float64) (counts []int) {
	counts = make([]int, len(notes))
	for _, n := range m {
		for _, v := range n {
			counts[numTo12(v)%12]++
		}
	}
	return
}

func loadTLI(fname string) (err error) {
	b, err := ioutil.ReadFile(fname)
	if err != nil {
		return
	}
	var data Data
	err = json.Unmarshal(b, &data)
	if err != nil {
		return
	}
	fmt.Println(data.FullText)

	// lets determine the key
	re := regexp.MustCompile("[^a-zA-Z#]")
	notes := []string{}
	for pattern := range data.Patterns {
		for _, pos := range data.Patterns[pattern].Parsed.Positions {
			for _, note := range pos.ParsedNotes {
				notes = append(notes, re.ReplaceAllString(note.N, ""))
			}
		}
	}
	fmt.Println("Key:", Key(notes))

	text := data.FullText
	for _, patternName := range data.PatternChain {
		text, err = processPattern(text, data.Patterns[patternName])
	}

	return
}

func processPattern(text string, pattern Pattern) (text2 string, err error) {
	text2 = text
	// find chords and add them to a matrix
	m := make([][]float64, 100)
	i := 0
	els := []string{}
	for _, pos := range pattern.Parsed.Positions {
		if len(pos.ParsedNotes) > 1 {
			els = append(els, pos.El)
			v := []float64{}
			for _, note := range pos.ParsedNotes {
				v = append(v, float64(math.Mod(float64(note.M), 12)))
			}
			m[i] = v
			i++
		}
	}

	for i, v := range m {
		if len(v) == 0 {
			if i == 0 {
				return
			}
			m = m[:i]
			break
		}
	}

	mMinNum, mMaxNum := rearrangeMatrix(m)
	fmt.Println(text)
	fmt.Println(els)
	mMin := assignNotes(mMinNum)
	mMax := assignNotes(mMaxNum)
	fmt.Println(mMin)
	fmt.Println("min")
	printMatrixS(mMin)
	fmt.Println("max")
	printMatrixS(mMax)

	return
}

func main() {
	err := loadTLI("../out.json")
	fmt.Println(err)
}

var majorKey = []float64{6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88}
var minorKey = []float64{6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17}

var notePositions = map[string]int{
	"C":  0,
	"D":  2,
	"E":  4,
	"F":  5,
	"G":  7,
	"A":  9,
	"B":  11,
	"C#": 1,
	"D#": 3,
	"E#": 5,
	"F#": 6,
	"G#": 8,
	"A#": 10,
	"B#": 0,
	"Db": 1,
	"Eb": 3,
	"Fb": 4,
	"Gb": 6,
	"Ab": 8,
	"Bb": 10,
}

func getKey(notes []string, major bool) (key string, rcoeff float64) {
	hasSharp := false
	for _, note := range notes {
		if strings.Contains(note, "#") {
			hasSharp = true
			break
		}
	}
	rs := make([]float64, 12)
	for level := 0; level < 12; level++ {
		counts := make([]float64, 12)
		for i := range counts {
			counts[i] = 0
		}
		for _, note := range notes {
			counts[(notePositions[note]+12-level)%12]++
		}
		if major {
			rs[level] = correlationCoefficient(majorKey, counts)
		} else {
			rs[level] = correlationCoefficient(minorKey, counts)
		}
	}

	max := 0.0
	maxI := 0
	for i, r := range rs {
		if r > max {
			maxI = i
			max = r
		}
	}
	key = "???"
	for note := range notePositions {
		if strings.HasSuffix(note, "#") && !hasSharp {
			continue
		}
		if strings.HasSuffix(note, "b") && hasSharp {
			continue
		}
		if notePositions[note] == maxI && len(note) < len(key) {
			key = note
		}
	}
	rcoeff = max
	return
}

func MajorKey(notes []string) (key string) {
	key, _ = getKey(notes, true)
	return
}

func Key(notes []string) (key string) {
	majorKey, majorR := getKey(notes, true)
	minorKey, minorR := getKey(notes, false)
	if majorR > minorR {
		key = majorKey
	} else {
		key = minorKey + "m"
	}
	return
}

func average(ns []float64) float64 {
	total := 0.0
	for _, n := range ns {
		total += n
	}
	return total / float64(len(ns))
}

func correlationCoefficient(x, y []float64) float64 {
	xmean := average(x)
	ymean := average(y)
	rtop := 0.0
	rbl := 0.0
	rbr := 0.0
	for i := range x {
		rtop += (x[i] - xmean) * (y[i] - ymean)
		rbl += math.Pow((x[i] - xmean), 2)
		rbr += math.Pow((y[i] - ymean), 2)
	}
	return rtop / math.Sqrt(rbl*rbr)
}
