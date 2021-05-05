package kr.ac.konkuk.mygetnews

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.ac.konkuk.mygetnews.databinding.ActivityMainBinding
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var adapter: MyAdapter

    val url = "https://www.daum.net"
    val rssurl = "https://fs.jtbc.joins.com//RSS/culture.xml"
    val jsonurl = "http://api.icndb.com/jokes/random"
    val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    private fun init() {

        //당겼을 때 수행할 기능 구현
        binding.swipe.setOnRefreshListener {
            //데이터 로딩완료시 프로그래스바를 다시 안보이게 하는 작업 필요 -> getNews()안에 있음
            binding.swipe.isRefreshing = true
            //새로고침 구현 -> 다시 사이트에 접속해서 데이터 긁어오기

            //json 실행시 둘중 하나 주석 지우고 실행
//            getNews()
            getRSSNews()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        //데코 추가 (row마다 구분자를 넣어준느 작업)
        //이런 기능도 있구나 ㅇㅅㅇ
        binding.recyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
        adapter = MyAdapter(ArrayList<MyData>())

        //인터페이스가 맴버로 있었기 때문에 맴버에 해당하는 정보 객체로 만들어서 세팅
        adapter.itemClickListener = object:MyAdapter.OnItemClickListener{
            override fun OnItemClick(
                holder: MyAdapter.MyViewHolder,
                view: View,
                data: MyData,
                position: Int
            ) {
                //adapter가 가지고 있는 포지션에 해당하는 item의 url로 이동
                val intent = Intent(ACTION_VIEW, Uri.parse(adapter.items[position].url))
                startActivity(intent)
            }

        }
        binding.recyclerView.adapter = adapter
        //news를 가져오는 작업 (jsoup을 이용-> 네트워크 이용 -> Dispatchers.IO)
        //메인스레드에선 할 수 없는 작업이기 때문에 coroutine 사용
//        getNews()
//        getRSSNews()
          getjson()
    }

    //xml
    fun getjson(){
        scope.launch {
            //배열에 저장되어있는 아이템들 제거(새로고침 시 똑같은 객체가 반복적으로 더해지는 문제 해결)
            //parser를 지정안해줄 경우 default가 html임
            //ignoreContentType(true) -> xml이나 html아니어도 읽어올 수 있도록 -> json도 받아오도록
            val doc = Jsoup.connect(jsonurl).ignoreContentType(true).get()
            val json = JSONObject(doc.text())
            val joke = json.getJSONObject("value")
            val jokestr = joke.getString("joke")
            Log.i("json joke", jokestr)

        }
    }


    //xml
    fun getRSSNews(){
        scope.launch {
            //배열에 저장되어있는 아이템들 제거(새로고침 시 똑같은 객체가 반복적으로 더해지는 문제 해결)
            adapter.items.clear()

            //parser를 지정안해줄 경우 default가 html임
            val doc = Jsoup.connect(rssurl).parser(Parser.xmlParser()).get()
            //> -> 아래 라는 의미 안에라고 해석해도 되겠다 html문서 참고 (웹페이지 -> 우클릭 페이지 소스보기))
            val headlines = doc.select("item")
            for(news in headlines){
                //아이템 추가
                //새로고침 할때마다 MyData객체가 생성되어 add되고 있음 지워지지 않고 (지우는 작업 필요)
                adapter.items.add(MyData(news.select("title").text(), news.select("link").text()))
            }
            //ui가 변경되는 작업
            //Context를 변경해주는 작업 필요
            withContext(Dispatchers.Main){
                //메인에서 화면 갱신
                adapter.notifyDataSetChanged()
                //데이터 로딩 후 UI갱신 후 프로그래스바 visibility-> false
                binding.swipe.isRefreshing = false
            }
        }
    }

    //html
    fun getNews(){
        scope.launch {
            //배열에 저장되어있는 아이템들 제거(새로고침 시 똑같은 객체가 반복적으로 더해지는 문제 해결)
            adapter.items.clear()
            //html 태그 형태의 문서를 가져옴
            //그러기 위해선 파싱하고자할 사이트의 내용을 알아야함(가지고 올 부분 탐색)
            //일반 뉴스이므로 ctrl + f 일반
            //href 에 해당하는 뉴스 링크와 뉴스 텍스트를 가져올 것
            val doc = Jsoup.connect(url).get()
            //> -> 아래 라는 의미 안에라고 해석해도 되겠다 html문서 참고 (웹페이지 -> 우클릭 페이지 소스보기))
            val headlines = doc.select("ul.list_txt>li>a")
            for(news in headlines){
                //아이템 추가
                //news.text() -> 앵커 태그(a)에 해당하는 텍스트  <a text </a>
                //새로고침 할때마다 MyData객체가 생성되어 add되고 있음 지워지지 않고 (지우는 작업 필요)
                adapter.items.add(MyData(news.text(), news.absUrl("href")))
            }
            //ui가 변경되는 작업
            //Context를 변경해주는 작업 필요
            withContext(Dispatchers.Main){
                //메인에서 화면 갱신
                adapter.notifyDataSetChanged()
                //데이터 로딩 후 UI갱신 후 프로그래스바 visibility-> false
                binding.swipe.isRefreshing = false
            }
        }
    }
}