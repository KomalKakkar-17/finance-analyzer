$ErrorActionPreference='Stop'
$repo='C:/Users/marus/finance-analyzer'
$out=Join-Path $repo 'documentation/uc503_endsem/revised_final/evidence'
$tmp='C:/Users/marus/AppData/Local/Temp/finance-report-revision/tests'
New-Item -ItemType Directory -Path $tmp -Force | Out-Null
$paths=Get-ChildItem -Path "$repo/src" -Recurse -File
$paths+=Get-Item "$repo/pom.xml"
$paths+=Get-ChildItem "$repo/data" -File
$paths|ForEach-Object {[PSCustomObject]@{path=$_.FullName;sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash}}|ConvertTo-Json -Depth 5|Set-Content "$out/application_baseline.json" -Encoding utf8
$jars=Get-ChildItem -Path "$env:USERPROFILE/.m2/repository/org/openjfx","$env:USERPROFILE/.m2/repository/org/xerial/sqlite-jdbc/3.46.1.0","$env:USERPROFILE/.m2/repository/org/mindrot/jbcrypt/0.4","$env:USERPROFILE/.m2/repository/org/junit","$env:USERPROFILE/.m2/repository/org/opentest4j","$env:USERPROFILE/.m2/repository/org/apiguardian","$env:USERPROFILE/.m2/repository/org/slf4j" -Recurse -Filter *.jar|Where-Object {$_.Name -notmatch 'sources|javadoc'}
$cp=$jars.FullName -join ';'
$sources=(Get-ChildItem "$repo/src/main/java" -Recurse -Filter *.java).FullName
& javac --release 21 -encoding UTF-8 -cp $cp -d $tmp $sources 2>&1|Out-File "$out/compile.txt" -Encoding utf8
"PRODUCTION_EXIT=$LASTEXITCODE; FILES=$($sources.Count)"|Add-Content "$out/compile.txt"
$tests=(Get-ChildItem "$repo/src/test/java" -Recurse -Filter *.java).FullName
& javac --release 21 -encoding UTF-8 -cp "$tmp;$cp" -d $tmp $tests 2>&1|Out-File "$out/test_compile.txt" -Encoding utf8
"TEST_COMPILE_EXIT=$LASTEXITCODE; FILES=$($tests.Count)"|Add-Content "$out/test_compile.txt"
$db=Join-Path $tmp ('isolated-'+[guid]::NewGuid().ToString('N')+'.db')
Get-Content "$repo/src/test/resources/app.properties"|ForEach-Object {$_ -replace '^db.url=.*$',('db.url=jdbc:sqlite:'+($db -replace '\\','/')) -replace '^log.file=.*$',('log.file='+$tmp+'/test.log')}|Set-Content "$tmp/app.properties" -Encoding ascii
@'
import java.util.*;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.*;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.MethodSource;
import com.financeanalyzer.util.ConfigLoader;
class ExistingSuite {
 public static void main(String[] args) throws Exception {
  String url=ConfigLoader.getProperty("db.url","");
  if(!url.contains("finance-report-revision/tests/isolated-")) throw new IllegalStateException("Unsafe database URL: "+url);
  System.out.println("ISOLATED_DATABASE|"+url);
  var config=new ConfigurationParameters(){public Optional<String> get(String k){return Optional.empty();}public Optional<Boolean> getBoolean(String k){return Optional.empty();}public int size(){return 0;}public Set<String> keySet(){return Set.of();}};
  List<DiscoverySelector> selectors=new ArrayList<>();
  for(String name:new String[]{"com.financeanalyzer.util.MoneyUtilTest","com.financeanalyzer.util.ValidatorsTest","com.financeanalyzer.service.HeuristicEngineTest","com.financeanalyzer.service.TransactionServiceTest","com.financeanalyzer.service.BudgetServiceTest","com.financeanalyzer.service.RatioAllocationServiceTest","com.financeanalyzer.dao.jdbc.JdbcUserFixedCategoryDaoTest"})selectors.add(DiscoverySelectors.selectClass(name));
  var request=new EngineDiscoveryRequest(){public <T extends DiscoverySelector>List<T> getSelectorsByType(Class<T> t){return selectors.stream().filter(t::isInstance).map(t::cast).toList();}public <T extends DiscoveryFilter<?>>List<T> getFiltersByType(Class<T> t){return List.of();}public ConfigurationParameters getConfigurationParameters(){return config;}};
  int[] counts=new int[3];
  var listener=new EngineExecutionListener(){public void executionFinished(TestDescriptor d,TestExecutionResult r){if(d.isTest()){String name=d.getSource().filter(MethodSource.class::isInstance).map(MethodSource.class::cast).map(m->m.getClassName()+"#"+m.getMethodName()).orElse(d.getDisplayName());String status=r.getStatus().name();counts[r.getStatus()==TestExecutionResult.Status.SUCCESSFUL?0:r.getStatus()==TestExecutionResult.Status.FAILED?1:2]++;System.out.println("CASE|"+status+"|"+name+"|"+r.getThrowable().map(Throwable::toString).orElse("Assertions completed"));}else if(r.getStatus()==TestExecutionResult.Status.FAILED){System.out.println("CONTAINER_FAILURE|"+d.getDisplayName()+"|"+r.getThrowable().map(Throwable::toString).orElse(""));}}};
  var engine=new JupiterTestEngine();var root=engine.discover(request,UniqueId.forEngine("junit-jupiter"));engine.execute(new ExecutionRequest(root,listener,config));
  System.out.println("TOTAL|"+counts[0]+"|"+counts[1]+"|"+counts[2]);
 }
}
'@|Set-Content "$tmp/ExistingSuite.java" -Encoding utf8
& java --class-path "$tmp;$cp" "$tmp/ExistingSuite.java" 2>&1|Out-File "$out/junit_results.txt" -Encoding utf8
"RUNNER_EXIT=$LASTEXITCODE"|Add-Content "$out/junit_results.txt"
Get-Content "$out/compile.txt","$out/test_compile.txt","$out/junit_results.txt"|Select-String -Pattern 'EXIT=|CASE\||TOTAL\||CONTAINER_FAILURE|ISOLATED_DATABASE'
