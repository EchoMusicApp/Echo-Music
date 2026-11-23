#!/usr/bin/env node
/**
 * Security Patch for Picomatch (GHSA-fc5q-39xc-vjpf / CVE-2026-112366)
 *
 * Vulnerability:
 * Picomatch through 4.0.7 contains a ReDoS vulnerability where analyzeRepeatedExtglob
 * in lib/parse.js skips single-branch repeated extglob patterns such as +(*) or +(*a),
 * compiling them with nested unbounded quantifiers and causing exponential backtracking.
 *
 * Upstream fix reference:
 * micromatch/picomatch PR #237
 */

import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const rootDir = path.resolve(__dirname, '..')

const isSelfRepeatingStarBranchCode = `
const isSelfRepeatingStarBranch = branch => {
  let escaped = false;
  let bracket = false;
  let star = false;

  for (let i = 0; i < branch.length; i++) {
    const ch = branch[i];

    if (escaped === true) {
      escaped = false;
      continue;
    }

    if (ch === '\\\\') {
      escaped = true;
      continue;
    }

    if (bracket === true) {
      if (ch === ']') bracket = false;
      continue;
    }

    if (ch === '[') {
      bracket = true;
      continue;
    }

    if (/[/(){}|!+@]/.test(ch)) {
      return false;
    }

    if (ch === '*') {
      if (branch[i + 1] === '*') {
        return false;
      }
      star = true;
    }
  }

  return star && !escaped && !bracket;
};
`

function findParseFiles(dir, files = []) {
  if (!fs.existsSync(dir)) return files
  const entries = fs.readdirSync(dir, { withFileTypes: true })
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      if (entry.name === '.git') continue
      findParseFiles(fullPath, files)
    } else if (
      entry.isFile() &&
      entry.name === 'parse.js' &&
      fullPath.includes(path.join('picomatch', 'lib'))
    ) {
      files.push(fullPath)
    }
  }
  return files
}

function patchPicomatchFile(filePath) {
  let content = fs.readFileSync(filePath, 'utf8')

  // Check if already patched
  if (content.includes('isSelfRepeatingStarBranch')) {
    console.log(`[security-patch] Already patched: ${filePath}`)
    return true
  }

  // 1. Insert helper function after getStarExtglobSequenceChars
  const target1 = 'return chars;\n};'
  const target1Crlf = 'return chars;\r\n};'

  let foundTarget1 = false
  if (content.includes(target1Crlf)) {
    content = content.replace(target1Crlf, target1Crlf + '\r\n' + isSelfRepeatingStarBranchCode)
    foundTarget1 = true
  } else if (content.includes(target1)) {
    content = content.replace(target1, target1 + '\n' + isSelfRepeatingStarBranchCode)
    foundTarget1 = true
  }

  // 2. Collapse single branch in analyzeRepeatedExtglob
  const target2 = 'if (sawStarSequence) {'
  const collapseBlock = `  if (branches.length === 1 && isSelfRepeatingStarBranch(branches[0])) {\n    return { risky: false, collapse: true };\n  }\n\n`

  const target2Idx = content.indexOf(target2)
  let foundTarget2 = false
  if (target2Idx !== -1) {
    const returnFalseIdx = content.indexOf('return { risky: false };', target2Idx)
    if (returnFalseIdx !== -1) {
      content = content.slice(0, returnFalseIdx) + collapseBlock + content.slice(returnFalseIdx)
      foundTarget2 = true
    }
  }

  // 3. Update token.close in extglobClose inside parse()
  const target3 = "let output = token.close + (opts.capture ? ')' : '');"
  const collapseCloseCheck = `if (analysis.collapse === true && (token.type === 'plus' || token.type === 'star')) {\n      token.close = token.type === 'plus' ? ')' : ')?';\n    }\n\n    `

  let foundTarget3 = false
  if (content.includes(target3)) {
    content = content.replace(target3, collapseCloseCheck + target3)
    foundTarget3 = true
  }

  if (foundTarget1 && foundTarget2 && foundTarget3) {
    fs.writeFileSync(filePath, content, 'utf8')
    console.log(`[security-patch] Successfully patched: ${filePath}`)
    return true
  } else {
    console.warn(
      `[security-patch] Failed to patch ${filePath}: target1=${foundTarget1}, target2=${foundTarget2}, target3=${foundTarget3}`,
    )
    return false
  }
}

const nodeModulesDir = path.join(rootDir, 'node_modules')
const parseFiles = findParseFiles(nodeModulesDir)

if (parseFiles.length === 0) {
  console.log('[security-patch] No picomatch/lib/parse.js files found in node_modules.')
} else {
  for (const file of parseFiles) {
    patchPicomatchFile(file)
  }
}
