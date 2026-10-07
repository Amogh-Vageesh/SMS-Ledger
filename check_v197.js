const fs=require('fs');
const html=fs.readFileSync('app/src/main/assets/index.html','utf8');
const gradle=fs.readFileSync('app/build.gradle.kts','utf8');
const readme=fs.readFileSync('README.md','utf8');
const checks=[
 ['versionName 1.97',/versionName\s*=\s*"1\.97"/.test(gradle)],
 ['versionCode 92',/versionCode\s*=\s*92/.test(gradle)],
 ['unified typography',html.includes('v1.97 unified typography system') && html.includes('--app-font')],
 ['Home Total Balance',html.includes('homeFinancialBalance') && html.includes('Total Balance')],
 ['investment/EPF wording',html.includes('investment, EPF/PF and pension balances')],
 ['credit-card limits excluded',html.includes('Credit-card limits are excluded')],
 ['promotional filtering',html.includes('Hard promotional markers')],
 ['self-transfer exclusion',html.includes('return "Self transfer"')],
 ['card payment exclusion',html.includes('Card payments')],
 ['mandate stop detection',html.includes('MANDATE_STOP')],
 ['mandate setup detection',html.includes('MANDATE_SETUP')],
 ['Mark paid',html.includes('data-paid') && html.includes('b.paid=true')],
 ['Insurance',html.includes('renderInsurance') && html.includes('state.insurance')],
 ['Family display names',html.includes('Only the person\'s chosen/display name is shown')],
 ['expandable asset sections',html.includes('collapsible-pane') && html.includes('<details class="asset-pane collapsible-pane"')],
 ['closed assets',html.includes('assetFilter==="Closed"')],
 ['family finance dedupe',html.includes('combinedFamilyFinanceState') && html.includes('assetSeen') && html.includes('acctSeen')],
 ['Google SHA-1 diagnostic',html.includes('SHA-1 unavailable')],
];
let bad=0; for(const [n,ok] of checks){console.log((ok?'PASS':'FAIL')+' '+n); if(!ok)bad++;}
console.log(`\n${checks.length-bad}/${checks.length} static checks passed`); process.exitCode=bad?1:0;
