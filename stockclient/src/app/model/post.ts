export interface Post {
    redditId: string;
   title: string;
   selfText: string;
   subReddit: string;
   author: string;
   score: number;
   upvoteRatio: number;
   numComments: number;
   createdTime: Date;
   flairText: string;
   postUrl: string;
   ticker: string;
}